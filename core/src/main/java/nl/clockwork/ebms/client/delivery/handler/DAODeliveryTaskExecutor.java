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
package nl.clockwork.ebms.client.delivery.handler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import nl.clockwork.ebms.client.client.DeliveryTaskDispatcher;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskDAO;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Polls {@link DeliveryTaskDAO} for pending delivery tasks and dispatches them, ensuring that across a cluster at most one worker polls at any time. The
 * single-writer guarantee is provided by a {@link LockProvider} (ShedLock, JDBC-backed) rather than by JGroups Raft: the executor acquires the
 * {@link #LOCK_NAME} lock for the duration of each poll cycle and releases it afterwards. The lease is renewed (extended) before each awaited task future, so a
 * slow cycle cannot let it lapse while the worker is still inside the critical section; if renewal reports the lease was lost, the worker stops waiting and
 * never calls {@code unlock()}, since the JDBC provider's unlock has no owner check and a stale holder could otherwise release another node's live lock. A
 * crashed worker keeps the lock until {@code lockAtMostFor} elapses, after which another worker takes over.
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class DAODeliveryTaskExecutor implements Runnable, ApplicationListener<ContextRefreshedEvent>, Ordered, DisposableBean
{
	private static final long DEFAULT_LEADER_CHECK_INTERVAL_MILLIS = 1_000L;
	private static final long DEFAULT_TASK_AWAIT_TIMEOUT_MILLIS = 60_000L;
	/**
	 * Safety window: how long a crashed worker's lock is kept before another worker may take over. Only relevant when the process dies without releasing the
	 * lock; a healthy worker releases the lock at the end of every cycle.
	 */
	private static final long DEFAULT_LOCK_AT_MOST_FOR_MILLIS = 5 * 60_000L;
	private static final int WORKER_AWAIT_TERMINATION_SECONDS = 30;
	private static final String LOCK_NAME = "ebms-delivery-task-executor";
	/**
	 * Ordered after the Flyway migration listener (which runs at {@code @Order(0)}) so the poller only starts once the schema (including the shedlock table)
	 * exists. The first poll used to run from the constructor, ahead of the migration, on a fresh database ({@code Table "DELIVERY_TASK" not found}).
	 */
	static final int START_ORDER = 1;

	@NonNull
	DeliveryTaskDAO deliveryTaskDAO;
	@NonNull
	DeliveryTaskDispatcher dispatcher;
	@NonNull
	LockProvider lockProvider;
	@NonNull
	TimedTask timedTask;
	int maxTasks;
	String serverId;
	long leaderCheckIntervalMillis;
	long taskAwaitTimeoutMillis;
	long lockAtMostForMillis;
	ThreadPoolTaskExecutor workerExecutor;
	final AtomicBoolean running = new AtomicBoolean();

	@Builder
	public DAODeliveryTaskExecutor(
			@NonNull DeliveryTaskDAO deliveryTaskDAO,
			@NonNull DeliveryTaskDispatcher dispatcher,
			@NonNull LockProvider lockProvider,
			@NonNull TimedTask timedTask,
			int maxTasks,
			String serverId,
			long leaderCheckIntervalMillis,
			long taskAwaitTimeoutMillis,
			long lockAtMostForMillis)
	{
		this.deliveryTaskDAO = deliveryTaskDAO;
		this.dispatcher = dispatcher;
		this.lockProvider = lockProvider;
		this.timedTask = timedTask;
		this.maxTasks = maxTasks;
		this.serverId = serverId;
		this.leaderCheckIntervalMillis = leaderCheckIntervalMillis > 0 ? leaderCheckIntervalMillis : DEFAULT_LEADER_CHECK_INTERVAL_MILLIS;
		this.taskAwaitTimeoutMillis = taskAwaitTimeoutMillis > 0 ? taskAwaitTimeoutMillis : DEFAULT_TASK_AWAIT_TIMEOUT_MILLIS;
		this.lockAtMostForMillis = lockAtMostForMillis > 0 ? lockAtMostForMillis : DEFAULT_LOCK_AT_MOST_FOR_MILLIS;
		// a single awaited future must complete strictly within the lease, otherwise the gate can
		// lapse while this worker is still inside the critical section
		if (this.taskAwaitTimeoutMillis >= this.lockAtMostForMillis)
			throw new IllegalArgumentException(
					"taskAwaitTimeoutMillis (" + this.taskAwaitTimeoutMillis + ") must be less than lockAtMostForMillis (" + this.lockAtMostForMillis + ")");
		this.workerExecutor = new ThreadPoolTaskExecutor();
		workerExecutor.setDaemon(true);
		workerExecutor.setMaxPoolSize(1);
		workerExecutor.setWaitForTasksToCompleteOnShutdown(false);
		workerExecutor.setAwaitTerminationSeconds(WORKER_AWAIT_TERMINATION_SECONDS);
		workerExecutor.afterPropertiesSet();
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		// Deferred from the constructor so the worker only starts once the context is fully refreshed.
		// The Flyway migration also runs on ContextRefreshedEvent but is ordered earlier, so the schema
		// is guaranteed to exist before the first poll.
		if (running.compareAndSet(false, true))
			workerExecutor.execute(this);
	}

	@Override
	public int getOrder()
	{
		return START_ORDER;
	}

	@Override
	public void destroy()
	{
		if (running.compareAndSet(true, false))
			workerExecutor.shutdown();
	}

	@Override
	public void run()
	{
		while (running.get() && !Thread.currentThread().isInterrupted())
		{
			Optional<SimpleLock> acquired = acquireLock();
			if (acquired.isPresent())
			{
				Lease lease = new Lease(acquired.get());
				try
				{
					runLeaderCycle(lease);
				}
				finally
				{
					// never unlock() after the lease was lost: the JDBC provider does not verify the
					// owner, so releasing would reset lock_until and could free another node's live lock
					if (lease.held())
						releaseLock(lease.lock);
					else
						log.warn("Delivery-task lock lost mid-cycle; leaving it to the current holder to lapse after {} ms", lockAtMostForMillis);
				}
			}
			else if (!sleep(leaderCheckIntervalMillis))
				return;
		}
	}

	private Optional<SimpleLock> acquireLock()
	{
		try
		{
			return lockProvider.lock(new LockConfiguration(Instant.now(), LOCK_NAME, Duration.ofMillis(lockAtMostForMillis), Duration.ZERO));
		}
		catch (RuntimeException e)
		{
			log.error("Failed to acquire delivery-task lock", e);
			return Optional.empty();
		}
	}

	private void releaseLock(SimpleLock lock)
	{
		try
		{
			lock.unlock();
		}
		catch (RuntimeException e)
		{
			log.error("Failed to release delivery-task lock; it will lapse after {} ms", lockAtMostForMillis, e);
		}
	}

	private void runLeaderCycle(Lease lease)
	{
		try
		{
			timedTask.run(() -> awaitAll(pollAndDispatch(), lease));
		}
		catch (RuntimeException e)
		{
			log.error("Delivery task polling cycle failed", e);
		}
	}

	private List<Future<?>> pollAndDispatch()
	{
		val futures = new ArrayList<Future<?>>();
		try
		{
			val timestamp = Instant.now();
			val tasks = maxTasks > 0 ? deliveryTaskDAO.getTasksBefore(timestamp, serverId, maxTasks) : deliveryTaskDAO.getTasksBefore(timestamp, serverId);
			tasks.forEach(task -> futures.add(dispatcher.dispatch(task)));
		}
		catch (RuntimeException e)
		{
			log.error("Failed to fetch or dispatch delivery tasks", e);
		}
		return futures;
	}

	private void awaitAll(List<Future<?>> futures, Lease lease)
	{
		for (int i = 0; i < futures.size(); i++)
		{
			if (!renewLease(lease))
			{
				lease.lost = true;
				cancelRemaining(futures, i);
				return;
			}
			val f = futures.get(i);
			try
			{
				f.get(taskAwaitTimeoutMillis, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				cancelRemaining(futures, i);
				return;
			}
			catch (TimeoutException e)
			{
				log.error("Delivery task did not complete within {} ms; cancelling", taskAwaitTimeoutMillis);
				f.cancel(true);
			}
			catch (CancellationException | ExecutionException e)
			{
				log.error("Delivery task execution failed", e);
			}
		}
	}

	/**
	 * Renews the lease so it cannot lapse during the next f.get() wait. extend() invalidates the old handle and returns a fresh one; an empty result, or a
	 * provider that does not support extension at all, is treated as lost, which errs towards leaving the lock to its holder.
	 */
	private boolean renewLease(Lease lease)
	{
		try
		{
			Optional<SimpleLock> extended = lease.lock.extend(Duration.ofMillis(lockAtMostForMillis), Duration.ZERO);
			extended.ifPresent(l -> lease.lock = l);
			return extended.isPresent();
		}
		catch (UnsupportedOperationException | IllegalStateException e)
		{
			log.warn("Delivery-task lock extension not applicable; treating the lease as lost", e);
			return false;
		}
		catch (RuntimeException e)
		{
			log.error("Failed to extend delivery-task lock", e);
			return false;
		}
	}

	/** Mutable holder for the current lock handle: extend() replaces it and lease.lost gates the final unlock(). */
	static final class Lease
	{
		volatile SimpleLock lock;
		volatile boolean lost;

		Lease(SimpleLock lock)
		{
			this.lock = lock;
		}

		boolean held()
		{
			return !lost;
		}
	}

	private static void cancelRemaining(List<Future<?>> futures, int from)
	{
		for (int i = from; i < futures.size(); i++)
			futures.get(i).cancel(true);
	}

	private static boolean sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
			return true;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
