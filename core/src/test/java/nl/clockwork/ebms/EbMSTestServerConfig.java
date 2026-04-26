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
package nl.clockwork.ebms;

import nl.clockwork.ebms.api.cpa.CPAControllerConfig;
import nl.clockwork.ebms.api.cpa.certificate.CertificateMappingControllerConfig;
import nl.clockwork.ebms.api.cpa.url.URLMappingControllerConfig;
import nl.clockwork.ebms.api.ebms.EbMSControllerConfig;
import nl.clockwork.ebms.client.delivery.DeliveryManagerConfig;
import nl.clockwork.ebms.client.delivery.client.EbMSClientConfig;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskHandlerConfig;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskManagerConfig;
import nl.clockwork.ebms.common.cpa.CPAConfig;
import nl.clockwork.ebms.common.cpa.certificate.CertificateMappingConfig;
import nl.clockwork.ebms.common.cpa.url.URLMappingConfig;
import nl.clockwork.ebms.dao.DAOConfig;
import nl.clockwork.ebms.datasource.DataSourceConfig;
import nl.clockwork.ebms.encryption.EncryptionConfig;
import nl.clockwork.ebms.event.MessageEventListenerConfig;
import nl.clockwork.ebms.jms.JMSConfig;
import nl.clockwork.ebms.processor.EbMSProcessorConfig;
import nl.clockwork.ebms.scheduler.SchedulerConfig;
import nl.clockwork.ebms.security.KeyStoreConfig;
import nl.clockwork.ebms.server.EbMSServerConfig;
import nl.clockwork.ebms.signing.SigningConfig;
import nl.clockwork.ebms.transaction.TransactionManagerConfig;
import nl.clockwork.ebms.validation.ValidationConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({PropertiesConfig.class, CertificateMappingConfig.class, CertificateMappingControllerConfig.class, CommonConfig.class, CPAConfig.class, CPAControllerConfig.class,
		DAOConfig.class, DataSourceConfig.class, DeliveryManagerConfig.class, DeliveryTaskManagerConfig.class, DeliveryTaskHandlerConfig.class,
		EbMSClientConfig.class, EbMSControllerConfig.class, EbMSProcessorConfig.class, EbMSServerConfig.class, EmbeddedWebConfig.class,
		EncryptionConfig.class, JMSConfig.class, KeyStoreConfig.class, MessageEventListenerConfig.class, SchedulerConfig.class, SigningConfig.class,
		TransactionManagerConfig.class, URLMappingConfig.class, URLMappingControllerConfig.class, ValidationConfig.class})
public class EbMSTestServerConfig
{

}
