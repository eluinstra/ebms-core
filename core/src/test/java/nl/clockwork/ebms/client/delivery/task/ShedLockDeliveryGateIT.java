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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import nl.clockwork.ebms.client.client.DeliveryTask;
import nl.clockwork.ebms.client.client.DeliveryTaskDispatcher;
import nl.clockwork.ebms.client.client.DeliveryTaskStatus;
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
 */
public class ShedLockDeliveryGateIT
{
	private static final String URL = "jdbc:h2:mem:shedlock-gate-it;DB_CLOSE_DELAY=-1";
	private static final String LOCK_NAME = "ebms-delivery-task-executor";
	private static final long TASK_AWAIT_TIMEOUT_MILLIS = 500;
	private static final long LEASE_MILLIS = 2_000;

	private SingleConnectionDataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	private DAODeliveryTaskExecutor executorA;
	private DAODeliveryTaskExecutor executorB;

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

	@Test
	void slowHolderRenewsTheGateAndOtherNodeOnlyPollsAfterRelease() throws Exception
	{
		// 30 due tasks; node A's dispatch is slow (300ms, still below the await timeout so the
		// cycle keeps futures in flight for ~9s and must renew the 2s lease before every await)
		for (int i = 1; i <= 30; i++)
			insertTask("msg-" + i);

		var dispatchesA = new CopyOnWriteArrayList<String>();
		var firstDispatchA = new CountDownLatch(1);
		// the slow send happens inside the dispatched future, i.e. in the awaitAll step that must renew
		executorA = executor(new DeliveryTaskDAOImpl(jdbcTemplate), task -> CompletableFuture.supplyAsync(() ->
		{
			dispatchesA.add(task.getMessageId());
			firstDispatchA.countDown();
			sleep(300);
			return null;
		}));

		var pollsB = new AtomicInteger();
		var dispatchesB = new CopyOnWriteArrayList<String>();
		var daoB = new CountingDAO(new DeliveryTaskDAOImpl(jdbcTemplate), pollsB);
		executorB = executor(daoB, task ->
		{
			dispatchesB.add(task.getMessageId());
			return CompletableFuture.completedFuture(null);
		});

		start(executorA);
		start(executorB);

		assertTrue(firstDispatchA.await(5, TimeUnit.SECONDS), "expected node A to start dispatching");
		// 4s > 2x the lease: without renewal the gate would have lapsed and B would have polled
		Thread.sleep(4_000);
		assertEquals(0, pollsB.get(), "node B must not enter getTasksBefore while the slow holder renews its lease");
		assertTrue(dispatchesA.size() > 1, "expected node A to still be working");
		assertTrue(dispatchesB.isEmpty(), "node B must not dispatch anything while node A holds the gate");
	}

	@Test
	void releasedGateIsTakenOverByTheOtherNode() throws Exception
	{
		insertTask("msg-1");
		var dispatchesB = new CopyOnWriteArrayList<String>();
		var doneB = new CountDownLatch(1);
		var pollsB = new AtomicInteger();
		var daoB = new CountingDAO(new DeliveryTaskDAOImpl(jdbcTemplate), pollsB);
		executorB = executor(daoB, task ->
		{
			dispatchesB.add(task.getMessageId());
			doneB.countDown();
			return CompletableFuture.completedFuture(null);
		});

		// a crashed holder left a long lease behind; force it expired to exercise takeover without
		// depending on node timing
		jdbcTemplate.update(
				"insert into shedlock (name, lock_until, locked_at, locked_by) values (?, ?, ?, ?)",
				LOCK_NAME,
				java.sql.Timestamp.from(Instant.now().minusSeconds(60)),
				java.sql.Timestamp.from(Instant.now()),
				"dead-node");

		start(executorB);
		assertTrue(doneB.await(10, TimeUnit.SECONDS), "expected the live node to take over the expired lease");
		assertEquals(List.of("msg-1"), dispatchesB);
		assertTrue(pollsB.get() > 0);
	}

	@Test
	void liveLeaseBlocksTheOtherNodeUntilItExpires() throws Exception
	{
		insertTask("msg-1");
		jdbcTemplate.update(
				"insert into shedlock (name, lock_until, locked_at, locked_by) values (?, ?, ?, ?)",
				LOCK_NAME,
				java.sql.Timestamp.from(Instant.now().plusSeconds(3)),
				java.sql.Timestamp.from(Instant.now()),
				"dead-node");

		var pollsB = new AtomicInteger();
		var dispatchesB = new CopyOnWriteArrayList<String>();
		var daoB = new CountingDAO(new DeliveryTaskDAOImpl(jdbcTemplate), pollsB);
		executorB = executor(daoB, task ->
		{
			dispatchesB.add(task.getMessageId());
			return CompletableFuture.completedFuture(null);
		});
		start(executorB);

		Thread.sleep(1_500);
		assertEquals(0, pollsB.get(), "node B must not poll while the (simulated) live lease stands");
		long deadline = System.currentTimeMillis() + 10_000;
		while (dispatchesB.isEmpty() && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		assertFalse(dispatchesB.isEmpty(), "node B must take over once the lease expires");
		assertEquals("msg-1", dispatchesB.get(0), "node B must take over once the lease expires");
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
				java.sql.Timestamp.from(Instant.now().minusSeconds(60)));
	}

	private DAODeliveryTaskExecutor executor(DeliveryTaskDAO dao, DeliveryTaskDispatcher dispatcher)
	{
		return DAODeliveryTaskExecutor.builder()
				.deliveryTaskDAO(dao)
				.dispatcher(dispatcher)
				.lockProvider(new JdbcLockProvider((DataSource)dataSource))
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

	/** Delegating DAO that counts getTasksBefore calls per node. */
	static class CountingDAO implements DeliveryTaskDAO
	{
		private final DeliveryTaskDAO delegate;
		private final AtomicInteger polls;

		CountingDAO(DeliveryTaskDAO delegate, AtomicInteger polls)
		{
			this.delegate = delegate;
			this.polls = polls;
		}

		@Override
		public List<DeliveryTask> getTasksBefore(Instant timestamp, String serverId)
		{
			polls.incrementAndGet();
			return delegate.getTasksBefore(timestamp, serverId);
		}

		@Override
		public List<DeliveryTask> getTasksBefore(Instant timestamp, String serverId, int maxNr)
		{
			polls.incrementAndGet();
			return delegate.getTasksBefore(timestamp, serverId, maxNr);
		}

		@Override
		public String insertTask(DeliveryTask task, String serverId)
		{
			return delegate.insertTask(task, serverId);
		}

		@Override
		public void insertLog(String messageId, Instant timestamp, String uri, DeliveryTaskStatus status, String errorMessage)
		{
			delegate.insertLog(messageId, timestamp, uri, status, errorMessage);
		}

		@Override
		public int updateTask(DeliveryTask task)
		{
			return delegate.updateTask(task);
		}

		@Override
		public int deleteTask(String messageId)
		{
			return delegate.deleteTask(messageId);
		}
	}
}
