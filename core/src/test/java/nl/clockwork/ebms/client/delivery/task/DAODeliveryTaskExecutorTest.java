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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import nl.clockwork.ebms.client.client.DeliveryTask;
import nl.clockwork.ebms.client.client.DeliveryTaskDispatcher;
import nl.clockwork.ebms.client.delivery.handler.DAODeliveryTaskExecutor;
import nl.clockwork.ebms.client.delivery.handler.TimedTask;
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
	LockProvider lockProvider;

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
		return build(executionIntervalMillis, taskAwaitTimeoutMillis, 10_000);
	}

	private DAODeliveryTaskExecutor build(long executionIntervalMillis, long taskAwaitTimeoutMillis, long lockAtMostForMillis)
	{
		return DAODeliveryTaskExecutor.builder()
				.deliveryTaskDAO(deliveryTaskDAO)
				.dispatcher(dispatcher)
				.lockProvider(lockProvider)
				.timedTask(new TimedTask(executionIntervalMillis))
				.maxTasks(0)
				.serverId("server-1")
				.leaderCheckIntervalMillis(20)
				.taskAwaitTimeoutMillis(taskAwaitTimeoutMillis)
				.lockAtMostForMillis(lockAtMostForMillis)
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
	void doesNotPollWhenLockNotAcquired() throws Exception
	{
		var lockAttempts = new CountDownLatch(5);
		when(lockProvider.lock(any(LockConfiguration.class))).thenAnswer(inv ->
		{
			lockAttempts.countDown();
			return Optional.empty();
		});
		executor = build(50, 200);
		startExecutor();
		assertTrue(lockAttempts.await(2, TimeUnit.SECONDS), "expected lock acquisition to be attempted several times");
		verify(deliveryTaskDAO, never()).getTasksBefore(any(Instant.class), any(String.class));
		verify(deliveryTaskDAO, never()).getTasksBefore(any(Instant.class), any(String.class), anyInt());
	}

	@Test
	void pollsAgainAfterLockRegained() throws Exception
	{
		var callCount = new AtomicInteger();
		when(lockProvider.lock(any(LockConfiguration.class))).thenAnswer(inv ->
		{
			int n = callCount.getAndIncrement();
			return (n == 0 || n >= 3) ? Optional.of(mock(SimpleLock.class)) : Optional.empty();
		});
		var polled = new CountDownLatch(2);
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv ->
		{
			polled.countDown();
			return Collections.emptyList();
		});
		executor = build(50, 200);
		startExecutor();
		assertTrue(polled.await(3, TimeUnit.SECONDS), "expected DAO to be polled at least twice across lock transitions");
	}

	@Test
	void cancelsFutureExceedingTimeout() throws Exception
	{
		when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.of(mock(SimpleLock.class)));
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
		when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.of(mock(SimpleLock.class)));
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
		when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.of(mock(SimpleLock.class)));
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
		when(lockProvider.lock(any(LockConfiguration.class))).thenAnswer(inv ->
		{
			refreshed.countDown();
			return Optional.of(mock(SimpleLock.class));
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

	@Test
	void renewsLeaseBeforeEachAwait() throws Exception
	{
		// A cycle can wait taskAwaitTimeoutMillis per future; the lease (lockAtMostFor) must be
		// renewed before each wait so a slow cycle cannot let another node take the gate.
		var extended = new CountDownLatch(2);
		var lock = new TestableSimpleLock();
		lock.onExtend = () ->
		{
			extended.countDown();
			return Optional.of(lock);
		};
		when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.of(lock));
		var tasks = new java.util.concurrent.atomic.AtomicReference<>(List.of(sampleTask(), sampleTask()));
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv -> tasks.getAndSet(Collections.emptyList()));
		var done = new CountDownLatch(2);
		when(dispatcher.dispatch(any(DeliveryTask.class))).thenAnswer(inv ->
		{
			done.countDown();
			return CompletableFuture.completedFuture(null);
		});
		executor = build(0, 200, 10_000);
		startExecutor();
		assertTrue(done.await(3, TimeUnit.SECONDS), "expected both tasks dispatched");
		assertTrue(extended.await(3, TimeUnit.SECONDS), "expected the lease extended before each awaited future");
	}

	@Test
	void doesNotUnlockAfterLeaseLoss() throws Exception
	{
		// When extend() reports the lease was lost mid-cycle, the executor must NOT unlock: unlock()
		// without ownership verification would reset lock_until and could release another node's live
		// lock. The cycle has two futures: the first extend succeeds, the second reports the loss.
		var lostLeaseLock = new TestableSimpleLock();
		var extendsLeft = new java.util.concurrent.atomic.AtomicInteger(1);
		lostLeaseLock.onExtend = () ->
		{
			// first renewal keeps the lease, the next one reports it lost to another node
			return extendsLeft.decrementAndGet() >= 0 ? Optional.of(lostLeaseLock) : Optional.<SimpleLock>empty();
		};
		var firstCycle = new java.util.concurrent.atomic.AtomicBoolean(true);
		when(lockProvider.lock(any(LockConfiguration.class)))
				.thenAnswer(inv -> firstCycle.getAndSet(false) ? Optional.of(lostLeaseLock) : Optional.<SimpleLock>empty());
		var dispatched = new CountDownLatch(2);
		var tasks = new java.util.concurrent.atomic.AtomicReference<>(List.of(sampleTask(), sampleTask()));
		when(deliveryTaskDAO.getTasksBefore(any(Instant.class), any(String.class))).thenAnswer(inv -> tasks.getAndSet(Collections.emptyList()));
		when(dispatcher.dispatch(any(DeliveryTask.class))).thenAnswer(inv ->
		{
			dispatched.countDown();
			return CompletableFuture.completedFuture(null);
		});
		executor = build(0, 300, 10_000);
		startExecutor();
		assertTrue(dispatched.await(3, TimeUnit.SECONDS), "expected both tasks dispatched");
		// give a (buggy) unconditional unlock a chance to run after the second extend failed
		Thread.sleep(500);
		assertFalse(lostLeaseLock.unlocked, "expected no unlock() after the lease was lost");
	}

	@Test
	void rejectsAwaitTimeoutNotBelowLease()
	{
		// Invariant: one awaited future must complete strictly within the lease, otherwise the
		// gate can lapse mid-wait. taskAwaitTimeoutMillis must stay below lockAtMostForMillis.
		assertThrows(
				IllegalArgumentException.class,
				() -> DAODeliveryTaskExecutor.builder()
						.deliveryTaskDAO(deliveryTaskDAO)
						.dispatcher(dispatcher)
						.lockProvider(lockProvider)
						.timedTask(new TimedTask(0))
						.maxTasks(0)
						.serverId("server-1")
						.leaderCheckIntervalMillis(20)
						.taskAwaitTimeoutMillis(300_000)
						.lockAtMostForMillis(10_000)
						.build());
	}

	/** Hand-rolled SimpleLock (not a mock of the behaviour under test) recording unlock/extend calls. */
	static class TestableSimpleLock implements SimpleLock
	{
		volatile boolean unlocked;
		volatile java.util.function.Supplier<Optional<SimpleLock>> onExtend = () -> Optional.of(this);

		@Override
		public void unlock()
		{
			unlocked = true;
		}

		@Override
		public Optional<SimpleLock> extend(Duration lockAtMostFor, Duration lockAtLeastFor)
		{
			return onExtend.get();
		}
	}
}
