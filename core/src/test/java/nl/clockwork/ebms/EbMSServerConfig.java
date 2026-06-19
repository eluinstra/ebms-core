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

import nl.clockwork.ebms.api.certificate.soap.CertificateMappingControllerConfig;
import nl.clockwork.ebms.api.cpa.soap.CPAControllerConfig;
import nl.clockwork.ebms.api.ebms.soap.EbMSControllerConfig;
import nl.clockwork.ebms.api.url.soap.URLMappingControllerConfig;
import nl.clockwork.ebms.client.delivery.DeliveryManagerConfig;
import nl.clockwork.ebms.client.delivery.handler.DeliveryTaskManagerConfig;
import nl.clockwork.ebms.client.transport.http.EbMSClientConfig;
import nl.clockwork.ebms.common.cpa.CPAConfig;
import nl.clockwork.ebms.common.cpa.certificate.CertificateMappingConfig;
import nl.clockwork.ebms.common.cpa.url.URLMappingConfig;
import nl.clockwork.ebms.common.dao.DAOConfig;
import nl.clockwork.ebms.common.datasource.DataSourceConfig;
import nl.clockwork.ebms.common.event.MessageEventListenerConfig;
import nl.clockwork.ebms.common.message.CommonConfig;
import nl.clockwork.ebms.common.security.EncryptionConfig;
import nl.clockwork.ebms.common.security.KeyStoreConfig;
import nl.clockwork.ebms.common.security.SigningConfig;
import nl.clockwork.ebms.common.transaction.TransactionManagerConfig;
import nl.clockwork.ebms.server.config.EbMSMessageProcessorConfig;
import nl.clockwork.ebms.server.security.certificate.ValidationConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({PropertiesConfig.class, EmbeddedWebConfig.class, nl.clockwork.ebms.server.config.EbMSServerConfig.class, EbMSControllerConfig.class,
		EbMSMessageProcessorConfig.class, MessageEventListenerConfig.class, CommonConfig.class, DeliveryManagerConfig.class, EbMSClientConfig.class,
		KeyStoreConfig.class, CertificateMappingConfig.class, CertificateMappingControllerConfig.class, DAOConfig.class, DeliveryTaskManagerConfig.class,
		ValidationConfig.class, EncryptionConfig.class, SigningConfig.class, CPAControllerConfig.class, CPAConfig.class, URLMappingConfig.class,
		URLMappingControllerConfig.class, DataSourceConfig.class, TransactionManagerConfig.class})
public class EbMSServerConfig
{

}
