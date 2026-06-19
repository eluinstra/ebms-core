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

import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.client.client.DeliveryTaskManager;
import nl.clockwork.ebms.client.delivery.EbMSDAO;
import nl.clockwork.ebms.client.delivery.task.DAODeliveryTaskManager;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskDAO;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskDAOImpl;
import nl.clockwork.ebms.common.cpa.CPAManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryTaskManagerConfig
{
	@Value("${ebms.serverId:#{null}}")
	String serverId;
	@Value("${deliveryTaskManager.nrAutoRetries}")
	int nrAutoRetries;
	@Value("${deliveryTaskManager.autoRetryInterval}")
	int autoRetryInterval;

	@Bean
	public DeliveryTaskManager deliveryTaskManager(DeliveryTaskDAO deliveryTaskDAO, EbMSDAO ebMSDAO, CPAManager cpaManager)
	{
		return new DAODeliveryTaskManager(ebMSDAO, deliveryTaskDAO, cpaManager, serverId, nrAutoRetries, autoRetryInterval);
	}

	@Bean
	public DeliveryTaskDAO deliveryTaskDAO(@org.jspecify.annotations.NonNull DataSource dataSource)
	{
		return new DeliveryTaskDAOImpl(new JdbcTemplate(dataSource));
	}
}
