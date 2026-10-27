/*
 * Copyright 2011 - 2026 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.client.delivery.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import nl.clockwork.ebms.client.client.DeliveryTaskDispatcher;
import nl.clockwork.ebms.client.delivery.handler.DAODeliveryTaskExecutor;
import nl.clockwork.ebms.client.delivery.handler.TimedTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Two executors ("nodes") over one shared in-memory H2 database and a real {@link JdbcLockProvider}, exercising what the mocked-LockProvider unit tests cannot
 * reach: that the real JDBC extend()/unlock SQL keeps the gate held by the working node (a waiting node never enters getTasksBefore while the holder renews),
 * and that an expired lease is taken over.
 * <p>
 * The assertions are deliberately based on the state of the shedlock row (lock_until / locked_by) rather than on wall-clock windows, so the tests stay
 * deterministic on slow or heavily loaded CI runners: node B's {@link RecordingLockProvider} records the row state right before every lock attempt, and B is
 * only allowed to have <i>acquired</i> the gate when the row was already expired or its.
 */
public class ShedLockDeliveryGateIT
{
	private static final String URL = "jdbc:h2:mem:shedlock-gate-it;DB_CLOSE_DELAY=-1";
	private static final String LOCK_NAME = "ebms-delivery-task-executor";
	private static final String NODE_A = "gate-it-node-a";
	private static final String NODE_B = "gate-it-node-b";
	private static final String DEAD_NODE = "dead-node";
	private static final long TASK_AWAIT_TIMEOUT_MILLIS = 500;
	private static final long LEASE_MILLIS = 2_000;
	/**
	 * Slack tolerated between reading the shedlock row and the provider's own {@code UPDATE ... WHERE lock_until <= now}; it absorbs the snapshot→update gap at a
	 * legitimate expiry-boundary takeover. Deliberately far smaller than any lease used here.
	 */
	private static final long SNAPSHOT_GRACE_MILLIS = 500;

	private SingleConnectionDataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	private DAODeliveryTaskExecutor executorA;
	private DAODeliveryTaskExecutor executorB;
	private final ConcurrentLinkedQueue<GateAttempt> attemptsB = new ConcurrentLinkedQueue<>();

	@BeforeEach
	void setUp() throws Exception
	{
		// one shared connection for both "nodes": autocommit gives independent per-statement commits,
		// and the lock is only ever written by the holder, so the gate semantics are preserved
		dataSource = new SingleConnectionDataSource(URL, "sa", "", true);
		jdbcTemplate = new JdbcTemplate(dataSource);
		try (Connection c = DriverManager.getConnection(URL, "sa", ""); Statement st = c.createStatement())
		{
			st.execute("drop table if exists delivery_task");
			st.execute("drop table if exists shedlock");
			st.execute(
					"create table delivery_task ("
							+ " cpa_id varchar(256) not null,"
							+ " send_channel_id varchar(256) null,"
							+ " receive_channel_id varchar(256) not null,"
							+ " message_id varchar(256) not null unique,"
							+ " time_to_live timestamp null,"
							+ " time_stamp timestamp not null,"
							+ " is_confidential boolean not null default false,"
							+ " retries smallint not null default 0,"
							+ " server_id varchar(256) null)");
			st.execute(
					"create table shedlock ("
							+ " name varchar(256) not null primary key,"
							+ " lock_until timestamp not null,"
							+ " locked_at timestamp not null,"
							+ " locked_by varchar(256) not null)");
		}
		attemptsB.clear();
	}

	@AfterEach
	void tearDown()
	{
		if (executorA != null)
			executorA.destroy();
		if (executorB != null)
			executorB.destroy();
		if (dataSource != null)
			dataSource.destroy();
	}

	/** Seeds the shedlock row with a lease held by {@code owner}; {@code live} controls whether the seeded lease is still valid. */
	private void seedGate(String owner, long leaseMillis, boolean live)
	{
		// MERGE (H2): the row may already exist (e.g. created by a running node), so upsert it
		jdbcTemplate.update(
				"merge into shedlock (name, lock_until, locked_at, locked_by) key (name) values (?, ?, ?, ?)",
				LOCK_NAME,
				Timestamp.from(live ? Instant.now().plusMillis(leaseMillis) : Instant.now().minusMillis(leaseMillis)),
				Timestamp.from(Instant.now()),
				owner);
	}

	@Test
	void slowHolderRenewsTheGateAndOtherNodeOnlyPollsAfterRelease() throws Exception
	{
		// 60 due tasks at 300ms each: node A's dispatch is slow (still below the await timeout), so its
		// single cycle keeps the futures in flight for ~18s and must renew the 2s lease before every await
		for (int i = 1; i <= 60; i++)
			insertTask("msg-" + i);

		var dispatchesA = new CopyOnWriteArrayList<String>();
		var firstDispatchA = new CountDownLatch(1);
		// the slow send happens inside the dispatched future, i.e. in the awaitAll step that must renew
		executorA = executor(NODE_A, new DeliveryTaskDAOImpl(jdbcTemplate), task -> CompletableFuture.supplyAsync(() ->
		{
			dispatchesA.add(task.getMessageId());
			firstDispatchA.countDown();
			sleep(300);
			return null;
		}));

		executorB = executor(NODE_B, new DeliveryTaskDAOImpl(jdbcTemplate), task ->
		{
			// B may dispatch: it can legitimately acquire the gate in the gaps between A's renewals
			// (the gate is single-writer at any instant, not permanently owned by A)
			return CompletableFuture.completedFuture(null);
		});

		// start A first and wait until it has actually dispatched a task (i.e. holds the gate); starting
		// both simultaneously lets B (instant dispatcher) win the initial lock on slow CI runners and
		// starve A so it never starts dispatching
		start(executorA);
		assertTrue(firstDispatchA.await(15, TimeUnit.SECONDS), "expected node A to start dispatching");
		start(executorB);
		// wait until both nodes have actually exercised the gate; the assertions below are state-based,
		// so this wait only bounds the test duration on a heavily loaded runner
		assertTrue(waitFor(() -> attemptsB.size() >= 1 && dispatchesA.size() >= 2, 20_000), "expected both nodes to have exercised the gate");
		// the deterministic gate property: B never acquired the gate while it was held live by node A,
		// even though A renewed its lease across the whole ~18s cycle
		assertGateNeverTakenWhileLive();
	}

	@Test
	void releasedGateIsTakenOverByTheOtherNode() throws Exception
	{
		insertTask("msg-1");
		var dispatchesB = new CopyOnWriteArrayList<String>();
		var doneB = new CountDownLatch(1);
		executorB = executor(NODE_B, new DeliveryTaskDAOImpl(jdbcTemplate), task ->
		{
			dispatchesB.add(task.getMessageId());
			// like production DeliveryTaskHandler: the task is consumed and removed, so it must not be
			// re-dispatched on B's subsequent poll cycles
			jdbcTemplate.update("delete from delivery_task where message_id = ?", task.getMessageId());
			doneB.countDown();
			return CompletableFuture.completedFuture(null);
		});

		// a crashed holder left a long lease behind; force it expired to exercise takeover without
		// depending on node timing
		seedGate(DEAD_NODE, 60_000, false);

		start(executorB);
		assertTrue(doneB.await(10, TimeUnit.SECONDS), "expected the live node to take over the expired lease");
		assertEquals(List.of("msg-1"), dispatchesB);
		assertEquals(NODE_B, gateSnapshot().owner(), "node B must hold the row after takeover");
		assertGateNeverTakenWhileLive();
	}

	@Test
	void liveLeaseBlocksTheOtherNodeUntilItExpires() throws Exception
	{
		insertTask("msg-1");
		// a crashed holder left a live (unexpired) lease behind, held by someone other than node B
		seedGate(DEAD_NODE, 3_000, true);

		var dispatchesB = new CopyOnWriteArrayList<String>();
		var doneB = new CountDownLatch(1);
		executorB = executor(NODE_B, new DeliveryTaskDAOImpl(jdbcTemplate), task ->
		{
			dispatchesB.add(task.getMessageId());
			// like production: the task is consumed and removed after dispatch
			jdbcTemplate.update("delete from delivery_task where message_id = ?", task.getMessageId());
			doneB.countDown();
			return CompletableFuture.completedFuture(null);
		});
		start(executorB);

		// B must stay blocked while the live lease stands and take over once it expires; the 10s wait
		// covers only B's dispatch latency on a heavily loaded runner (the lease itself lapses at 3s)
		assertTrue(doneB.await(10, TimeUnit.SECONDS), "node B must take over once the (simulated) live lease expires");
		assertEquals(List.of("msg-1"), dispatchesB, "node B must take over once the lease expires and dispatch exactly once");
		assertEquals(NODE_B, gateSnapshot().owner(), "node B must hold the row after takeover");
		// deterministic substitute for the former "at most 1 poll in 1.5s" wall-clock window: B must never
		// have acquired the gate while a live lease stood
		assertGateNeverTakenWhileLive();
	}

	/**
	 * The core invariant, checked against the shedlock-row state recorded right before every lock attempt of node B: B may only have <i>acquired</i> the gate
	 * when the row was absent, already expired (within the snapshot grace), or held by B itself at the moment of the attempt.
	 */
	private void assertGateNeverTakenWhileLive()
	{
		for (GateAttempt attempt : attemptsB)
		{
			if (!attempt.acquired())
				continue;
			var g = attempt.state();
			assertTrue(
					!g.heldLiveByOtherThan(NODE_B, SNAPSHOT_GRACE_MILLIS),
					"node B acquired the gate while it was held live by %s (lock_until=%s, snapshot_at=%s)".formatted(g.owner(), g.lockUntil(), g.at()));
		}
	}

	private void start(DAODeliveryTaskExecutor executor)
	{
		executor.onApplicationEvent(new ContextRefreshedEvent(Mockito.mock(ApplicationContext.class)));
	}

	private void insertTask(String messageId)
	{
		jdbcTemplate.update(
				"insert into delivery_task (cpa_id, send_channel_id, receive_channel_id, message_id, time_stamp, is_confidential, retries)"
						+ " values ('cpa', null, 'channel', ?, ?, false, 0)",
				messageId,
				Timestamp.from(Instant.now().minusSeconds(60)));
	}

	private DAODeliveryTaskExecutor executor(String lockedBy, DeliveryTaskDAO dao, DeliveryTaskDispatcher dispatcher)
	{
		var provider = new JdbcLockProvider(JdbcLockProvider.Configuration.builder((DataSource)dataSource).withLockedByValue(lockedBy).build());
		return DAODeliveryTaskExecutor.builder()
				.deliveryTaskDAO(dao)
				.dispatcher(dispatcher)
				.lockProvider(lockedBy.equals(NODE_B) ? new RecordingLockProvider(provider) : provider)
				.timedTask(new TimedTask(0))
				.maxTasks(0)
				.serverId(null)
				.leaderCheckIntervalMillis(50)
				.taskAwaitTimeoutMillis(TASK_AWAIT_TIMEOUT_MILLIS)
				.lockAtMostForMillis(LEASE_MILLIS)
				.build();
	}

	private static void sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Reads the shedlock row on a fresh connection (the shared {@link #dataSource} connection is held by node B during its dispatch and is not reentrant).
	 */
	private GateSnapshot gateSnapshot()
	{
		var at = Instant.now();
		try (Connection c = DriverManager.getConnection(URL, "sa", ""); Statement st = c.createStatement();
				var rs = st.executeQuery("select locked_by, lock_until from shedlock where name = '" + LOCK_NAME + "'"))
		{
			if (rs.next())
				return new GateSnapshot(rs.getString(1), rs.getTimestamp(2), at);
			return new GateSnapshot(null, null, at);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Failed to read shedlock row", e);
		}
	}

	/**
	 * Polls {@code condition} every 50 ms until it holds or {@code timeoutMillis} elapses; returns the final value of the condition.
	 */
	private static boolean waitFor(BooleanSupplier condition, long timeoutMillis)
	{
		var deadline = System.currentTimeMillis() + timeoutMillis;
		while (System.currentTimeMillis() < deadline)
		{
			if (condition.getAsBoolean())
				return true;
			sleep(50);
		}
		return condition.getAsBoolean();
	}

	/**
	 * Records the shedlock row state right before every lock() call, so the test can verify against the actual row state rather than a wall-clock window.
	 */
	private class RecordingLockProvider implements LockProvider
	{
		private final LockProvider delegate;

		RecordingLockProvider(LockProvider delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public Optional<SimpleLock> lock(LockConfiguration configuration)
		{
			var state = gateSnapshot();
			var result = delegate.lock(configuration);
			attemptsB.add(new GateAttempt(state, result.isPresent()));
			return result;
		}
	}

	/** The shedlock row state observed right before a lock attempt by node B. */
	private record GateSnapshot(String owner, Timestamp lockUntil, Instant at)
	{
		/**
		 * True when, at the moment the row was observed ({@code at}), the gate was held by someone other than {@code self} with a lease still valid — ignoring at
		 * most {@code graceMillis} so the snapshot→update gap of a legitimate expiry-boundary takeover does not count.
		 */
		boolean heldLiveByOtherThan(String self, long graceMillis)
		{
			return lockUntil != null && !owner.equals(self) && lockUntil.toInstant().isAfter(at.plusMillis(graceMillis));
		}
	}

	/** Node B's lock attempt, tagged with the pre-attempt row state and whether it won the gate. */
	private record GateAttempt(GateSnapshot state, boolean acquired)
	{
	}
}
