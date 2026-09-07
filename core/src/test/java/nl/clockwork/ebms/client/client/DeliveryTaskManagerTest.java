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
package nl.clockwork.ebms.client.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import nl.clockwork.ebms.client.delivery.EbMSDAO;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskDAO;
import nl.clockwork.ebms.client.transport.http.EbMSUnrecoverableResponseException;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.protocol.EbMSAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DeliveryTaskManagerTest
{
	@Mock
	EbMSDAO ebMSDAO;
	@Mock
	DeliveryTaskDAO deliveryTaskDAO;
	@Mock
	CPAManager cpaManager;

	AutoCloseable mocks;
	int nrAutoRetries;
	long autoRetryInterval;
	TestDeliveryTaskManager manager;

	@BeforeEach
	void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
		nrAutoRetries = 3;
		autoRetryInterval = 5;
		manager = new TestDeliveryTaskManager(ebMSDAO, deliveryTaskDAO, cpaManager, nrAutoRetries, autoRetryInterval);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		mocks.close();
	}

	private DeliveryTask task(int retries)
	{
		return DeliveryTask.builder().cpaId("cpaId").receiveDeliveryChannelId("recvChannel").messageId("msgId").timestamp(Instant.now()).retries(retries).build();
	}

	private void action(EbMSAction action)
	{
		when(ebMSDAO.getMessageAction("msgId")).thenReturn(Optional.of(action));
	}

	@Test
	void shouldRetryUnreliable_whenFailedAndUnrecoverableAndUnderLimit()
	{
		action(EbMSAction.ACKNOWLEDGMENT);
		assertTrue(manager.shouldRetryUnreliable(task(0), DeliveryTaskStatus.FAILED, new RuntimeException(), false));
	}

	@Test
	void shouldRetryUnreliable_whenMessageErrorAction()
	{
		action(EbMSAction.MESSAGE_ERROR);
		assertTrue(manager.shouldRetryUnreliable(task(1), DeliveryTaskStatus.FAILED, new RuntimeException(), false));
	}

	@Test
	void shouldNotRetryUnreliable_whenSucceeded()
	{
		action(EbMSAction.ACKNOWLEDGMENT);
		assertFalse(manager.shouldRetryUnreliable(task(0), DeliveryTaskStatus.SUCCEEDED, new RuntimeException(), false));
	}

	@Test
	void shouldNotRetryUnreliable_whenUnrecoverableException()
	{
		action(EbMSAction.ACKNOWLEDGMENT);
		assertFalse(manager.shouldRetryUnreliable(task(0), DeliveryTaskStatus.FAILED, mock(EbMSUnrecoverableResponseException.class), false));
	}

	@Test
	void shouldNotRetryUnreliable_whenReliableMessaging()
	{
		action(EbMSAction.ACKNOWLEDGMENT);
		assertFalse(manager.shouldRetryUnreliable(task(0), DeliveryTaskStatus.FAILED, new RuntimeException(), true));
	}

	@Test
	void shouldNotRetryUnreliable_whenRetriesExhausted()
	{
		action(EbMSAction.ACKNOWLEDGMENT);
		assertFalse(manager.shouldRetryUnreliable(task(nrAutoRetries), DeliveryTaskStatus.FAILED, new RuntimeException(), false));
	}

	@Test
	void shouldNotRetryUnreliable_whenNoMessageAction()
	{
		when(ebMSDAO.getMessageAction("msgId")).thenReturn(Optional.empty());
		assertFalse(manager.shouldRetryUnreliable(task(0), DeliveryTaskStatus.FAILED, new RuntimeException(), false));
	}

	@Test
	void shouldNotRetryUnreliable_whenUnrelatedAction()
	{
		action(EbMSAction.PING);
		assertFalse(manager.shouldRetryUnreliable(task(0), DeliveryTaskStatus.FAILED, new RuntimeException(), false));
	}

	private static class TestDeliveryTaskManager extends DeliveryTaskManager
	{
		TestDeliveryTaskManager(EbMSDAO ebMSDAO, DeliveryTaskDAO deliveryTaskDAO, CPAManager cpaManager, int nrAutoRetries, long autoRetryInterval)
		{
			super(ebMSDAO, deliveryTaskDAO, cpaManager, nrAutoRetries, autoRetryInterval);
		}

		@Override
		public void insertTask(DeliveryTask task)
		{
		}

		@Override
		public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status)
		{
		}

		@Override
		public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status, String errorMessage)
		{
		}

		@Override
		public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status, String errorMessage, Exception e)
		{
		}

		@Override
		public void deleteTask(String messageId)
		{
		}
	}
}
