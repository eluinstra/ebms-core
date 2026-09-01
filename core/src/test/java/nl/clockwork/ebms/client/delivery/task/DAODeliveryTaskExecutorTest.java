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
package nl.clockwork.ebms.client.delivery.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import nl.clockwork.ebms.client.client.DeliveryTask;
import nl.clockwork.ebms.client.client.DeliveryTaskDispatcher;
import nl.clockwork.ebms.client.delivery.handler.DAODeliveryTaskExecutor;
import nl.clockwork.ebms.client.delivery.handler.TimedTask;
import org.jgroups.raft.RaftHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

class DAODeliveryTaskExecutorTest
{
	@Mock
	DeliveryTaskDAO deliveryTaskDAO;
	@Mock
	DeliveryTaskDispatcher dispatcher;
	@Mock
	RaftHandle raftHandle;

	AutoCloseable mocks;
	DAODeliveryTaskExecutor executor;

	@BeforeEach
	void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (executor != null)
			executor.destroy();
		mocks.close();
	}

	private DAODeliveryTaskExecutor build(long executionIntervalMillis, long taskAwaitTimeoutMillis)
	{
		return DAODeliveryTaskExecutor.builder()
				.deliveryTaskDAO(deliveryTaskDAO)
				.dispatcher(dispatcher)
				.raftHandle(raftHandle)
				.timedTask(new TimedTask(executionIntervalMillis))
				.maxTasks(0)
				.serverId("server-1")
				.leaderCheckIntervalMillis(20)
				.taskAwaitTimeoutMillis(taskAwaitTimeoutMillis)
				.build();
	}

	private void startExecutor()
	{
		executor.onApplicationEvent(new ContextRefreshedEvent(mock(ApplicationContext.class)));
	}

	private static DeliveryTask sampleTask()
	{
		return DeliveryTask.builder().cpaId("cpa").receiveDeliveryChannelId("channel").messageId("msg-1").timestamp(Instant.now()).build();
	}

	@Test
	void doesNotPollWhenNotLeader() throws Exception
	{
		var leaderChecks = new CountDownLatch(5);
		when(raftHandle.isLeader()).thenAnswer(inv ->
		{
			leaderChecks.countDown();
			return false;
		});
		executor = build(50, 200);
		startExecutor();
		assertTrue(leaderChecks.await(2, TimeUnit.SECONDS), "expected raftHandle.isLeader() to be polled several times");
		verify(deliveryTaskDAO, never()).getTasksBefore(any(Instant.class), any(String.class));
		verify(deliveryTaskDAO, never()).getTasksBefore(any(Instant.class), any(String.class), anyInt());
	}

	@Test
	void pollsAgainAfterLeaderRegained() throws Exception
	{
		var callCount = new AtomicInteger();
		when(raftHandle.isLeader()).thenAnswer(inv ->
		{
			int n = callCount.getAndIncrement();
			return n == 0 || n >= 3;
		});
		var polled = new CountDownLatch(2);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			polled.countDown();
			return Collections.emptyList();
		});
		executor = build(50, 200);
		startExecutor();
		assertTrue(polled.await(3, TimeUnit.SECONDS), "expected DAO to be polled at least twice across leader transitions");
	}

	@Test
	void cancelsFutureExceedingTimeout() throws Exception
	{
		when(raftHandle.isLeader()).thenReturn(true);
		var task = sampleTask();
		var stuck = new CompletableFuture<Void>();
		var handed = new CountDownLatch(1);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			if (handed.getCount() > 0)
				return List.of(task);
			return Collections.emptyList();
		});
		lenient().when(dispatcher.dispatch(task)).thenAnswer(inv ->
		{
			handed.countDown();
			return stuck;
		});
		executor = build(0, 100);
		startExecutor();
		assertThrows(CancellationException.class, () -> stuck.get(3, TimeUnit.SECONDS));
	}

	@Test
	void destroyTerminatesWorkerQuickly() throws Exception
	{
		when(raftHandle.isLeader()).thenReturn(true);
		var polled = new CountDownLatch(1);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			polled.countDown();
			return Collections.emptyList();
		});
		executor = build(50, 200);
		startExecutor();
		assertTrue(polled.await(2, TimeUnit.SECONDS), "expected worker to start polling before destroy");
		var start = System.currentTimeMillis();
		executor.destroy();
		var elapsed = System.currentTimeMillis() - start;
		executor = null;
		assertTrue(elapsed < 5_000, "destroy() took too long: " + elapsed + " ms");
	}

	@Test
	void recoversFromDaoFailure() throws Exception
	{
		when(raftHandle.isLeader()).thenReturn(true);
		var invocations = new AtomicInteger();
		var polled = new CountDownLatch(2);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			int n = invocations.getAndIncrement();
			polled.countDown();
			if (n == 0)
				throw new RuntimeException("simulated DAO failure");
			return Collections.emptyList();
		});
		executor = build(50, 200);
		startExecutor();
		assertTrue(polled.await(3, TimeUnit.SECONDS), "expected polling to continue after a DAO exception");
	}

	@Test
	void doesNotPollBeforeContextRefreshed() throws Exception
	{
		// The worker used to start from the constructor, racing ahead of the Flyway migration on a
		// fresh database ("Table DELIVERY_TASK not found"). It must now wait for the context refresh.
		var refreshed = new CountDownLatch(1);
		when(raftHandle.isLeader()).thenAnswer(inv ->
		{
			refreshed.countDown();
			return true;
		});
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			refreshed.countDown();
			return Collections.emptyList();
		});
		executor = build(20, 200);
		// give a (buggy) eager worker a chance to start polling before the context is refreshed
		assertFalse(refreshed.await(1, TimeUnit.SECONDS), "expected the executor to remain idle before the context refresh");
		startExecutor();
		assertTrue(refreshed.await(2, TimeUnit.SECONDS), "expected the executor to start polling after the context refresh");
	}
}
