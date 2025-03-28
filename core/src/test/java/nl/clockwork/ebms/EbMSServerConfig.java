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

import nl.clockwork.ebms.cpa.CPAManagerConfig;
import nl.clockwork.ebms.cpa.CPAServiceConfig;
import nl.clockwork.ebms.cpa.certificate.CertificateMappingServiceConfig;
import nl.clockwork.ebms.cpa.url.URLMappingServiceConfig;
import nl.clockwork.ebms.dao.DAOConfig;
import nl.clockwork.ebms.datasource.DataSourceConfig;
import nl.clockwork.ebms.delivery.DeliveryManagerConfig;
import nl.clockwork.ebms.delivery.client.EbMSClientConfig;
import nl.clockwork.ebms.delivery.task.DeliveryTaskManagerConfig;
import nl.clockwork.ebms.encryption.EncryptionConfig;
import nl.clockwork.ebms.event.MessageEventListenerConfig;
import nl.clockwork.ebms.jms.JMSConfig;
import nl.clockwork.ebms.processor.EbMSProcessorConfig;
import nl.clockwork.ebms.security.KeyStoreConfig;
import nl.clockwork.ebms.service.EbMSMessageServiceConfig;
import nl.clockwork.ebms.signing.SigningConfig;
import nl.clockwork.ebms.transaction.TransactionManagerConfig;
import nl.clockwork.ebms.validation.ValidationConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({PropertiesConfig.class, EmbeddedWebConfig.class, nl.clockwork.ebms.server.EbMSServerConfig.class, EbMSMessageServiceConfig.class,
		EbMSProcessorConfig.class, MessageEventListenerConfig.class, CommonConfig.class, DeliveryManagerConfig.class, EbMSClientConfig.class, KeyStoreConfig.class,
		CertificateMappingServiceConfig.class, DAOConfig.class, DeliveryTaskManagerConfig.class, JMSConfig.class, ValidationConfig.class, EncryptionConfig.class,
		SigningConfig.class, CPAServiceConfig.class, CPAManagerConfig.class, URLMappingServiceConfig.class, DataSourceConfig.class, TransactionManagerConfig.class})
public class EbMSServerConfig
{

}
