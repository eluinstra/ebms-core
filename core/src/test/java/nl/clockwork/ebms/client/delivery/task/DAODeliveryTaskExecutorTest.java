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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
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
import lombok.val;
import org.jgroups.raft.RaftHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DAODeliveryTaskExecutorTest
{
	@Mock
	DeliveryTaskDAO deliveryTaskDAO;
	@Mock
	DeliveryTaskHandler deliveryTaskHandler;
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
				.deliveryTaskHandler(deliveryTaskHandler)
				.raftHandle(raftHandle)
				.timedTask(new TimedTask(executionIntervalMillis))
				.maxTasks(0)
				.serverId("server-1")
				.leaderCheckIntervalMillis(20)
				.taskAwaitTimeoutMillis(taskAwaitTimeoutMillis)
				.build();
	}

	private static DeliveryTask sampleTask()
	{
		return DeliveryTask.builder().cpaId("cpa").receiveDeliveryChannelId("channel").messageId("msg-1").timestamp(Instant.now()).build();
	}

	@Test
	void doesNotPollWhenNotLeader() throws Exception
	{
		val leaderChecks = new CountDownLatch(5);
		when(raftHandle.isLeader()).thenAnswer(inv ->
		{
			leaderChecks.countDown();
			return false;
		});
		executor = build(50, 200);
		assertTrue(leaderChecks.await(2, TimeUnit.SECONDS), "expected raftHandle.isLeader() to be polled several times");
		verify(deliveryTaskDAO, never()).getTasksBefore(any(Instant.class), any(String.class));
		verify(deliveryTaskDAO, never()).getTasksBefore(any(Instant.class), any(String.class), anyInt());
	}

	@Test
	void pollsAgainAfterLeaderRegained() throws Exception
	{
		val callCount = new AtomicInteger();
		when(raftHandle.isLeader()).thenAnswer(inv ->
		{
			int n = callCount.getAndIncrement();
			return n == 0 || n >= 3;
		});
		val polled = new CountDownLatch(2);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			polled.countDown();
			return Collections.emptyList();
		});
		executor = build(50, 200);
		assertTrue(polled.await(3, TimeUnit.SECONDS), "expected DAO to be polled at least twice across leader transitions");
	}

	@Test
	void cancelsFutureExceedingTimeout() throws Exception
	{
		when(raftHandle.isLeader()).thenReturn(true);
		val task = sampleTask();
		val stuck = new CompletableFuture<Void>();
		val handed = new CountDownLatch(1);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			if (handed.getCount() > 0)
				return List.of(task);
			return Collections.emptyList();
		});
		lenient().when(deliveryTaskHandler.handleAsync(task)).thenAnswer(inv ->
		{
			handed.countDown();
			return stuck;
		});
		executor = build(0, 100);
		assertThrows(CancellationException.class, () -> stuck.get(3, TimeUnit.SECONDS));
	}

	@Test
	void destroyTerminatesWorkerQuickly() throws Exception
	{
		when(raftHandle.isLeader()).thenReturn(true);
		val polled = new CountDownLatch(1);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			polled.countDown();
			return Collections.emptyList();
		});
		executor = build(50, 200);
		assertTrue(polled.await(2, TimeUnit.SECONDS), "expected worker to start polling before destroy");
		val start = System.currentTimeMillis();
		executor.destroy();
		val elapsed = System.currentTimeMillis() - start;
		executor = null;
		assertTrue(elapsed < 5_000, "destroy() took too long: " + elapsed + " ms");
	}

	@Test
	void recoversFromDaoFailure() throws Exception
	{
		when(raftHandle.isLeader()).thenReturn(true);
		val invocations = new AtomicInteger();
		val polled = new CountDownLatch(2);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			int n = invocations.getAndIncrement();
			polled.countDown();
			if (n == 0)
				throw new RuntimeException("simulated DAO failure");
			return Collections.emptyList();
		});
		executor = build(50, 200);
		assertTrue(polled.await(3, TimeUnit.SECONDS), "expected polling to continue after a DAO exception");
	}
}
