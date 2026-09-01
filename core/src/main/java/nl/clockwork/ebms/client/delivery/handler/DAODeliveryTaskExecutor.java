/*
 * Copyright 2011 Clockwork
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import nl.clockwork.ebms.client.client.DeliveryTaskDispatcher;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskDAO;
import org.jgroups.raft.RaftHandle;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class DAODeliveryTaskExecutor implements Runnable, ApplicationListener<ContextRefreshedEvent>, Ordered, DisposableBean
{
	private static final long DEFAULT_LEADER_CHECK_INTERVAL_MILLIS = 1_000L;
	private static final long DEFAULT_TASK_AWAIT_TIMEOUT_MILLIS = 60_000L;
	private static final int WORKER_AWAIT_TERMINATION_SECONDS = 30;
	/**
	 * Ordered after the Flyway migration listener (which runs at {@code @Order(0)}) so the poller only starts once the schema exists. The first poll used to run
	 * from the constructor, ahead of the migration, on a fresh database ({@code Table "DELIVERY_TASK" not found}).
	 */
	static final int START_ORDER = 1;

	@NonNull
	DeliveryTaskDAO deliveryTaskDAO;
	@NonNull
	DeliveryTaskDispatcher dispatcher;
	@NonNull
	RaftHandle raftHandle;
	@NonNull
	TimedTask timedTask;
	int maxTasks;
	String serverId;
	long leaderCheckIntervalMillis;
	long taskAwaitTimeoutMillis;
	ThreadPoolTaskExecutor workerExecutor;
	final AtomicBoolean running = new AtomicBoolean();

	@Builder
	public DAODeliveryTaskExecutor(
			@NonNull DeliveryTaskDAO deliveryTaskDAO,
			@NonNull DeliveryTaskDispatcher dispatcher,
			@NonNull RaftHandle raftHandle,
			@NonNull TimedTask timedTask,
			int maxTasks,
			String serverId,
			long leaderCheckIntervalMillis,
			long taskAwaitTimeoutMillis)
	{
		this.deliveryTaskDAO = deliveryTaskDAO;
		this.dispatcher = dispatcher;
		this.raftHandle = raftHandle;
		this.timedTask = timedTask;
		this.maxTasks = maxTasks;
		this.serverId = serverId;
		this.leaderCheckIntervalMillis = leaderCheckIntervalMillis > 0 ? leaderCheckIntervalMillis : DEFAULT_LEADER_CHECK_INTERVAL_MILLIS;
		this.taskAwaitTimeoutMillis = taskAwaitTimeoutMillis > 0 ? taskAwaitTimeoutMillis : DEFAULT_TASK_AWAIT_TIMEOUT_MILLIS;
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
		while (!Thread.currentThread().isInterrupted())
		{
			if (raftHandle.isLeader())
				runLeaderCycle();
			else if (!sleep(leaderCheckIntervalMillis))
				return;
		}
	}

	private void runLeaderCycle()
	{
		try
		{
			timedTask.run(() -> awaitAll(pollAndDispatch()));
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

	private void awaitAll(List<Future<?>> futures)
	{
		for (int i = 0; i < futures.size(); i++)
		{
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
