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
package nl.clockwork.ebms.server.embedded.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.api.certificate.soap.CertificateMappingControllerConfig;
import nl.clockwork.ebms.api.cpa.soap.CPAControllerConfig;
import nl.clockwork.ebms.api.ebms.soap.EbMSControllerConfig;
import nl.clockwork.ebms.api.url.soap.URLMappingControllerConfig;
import nl.clockwork.ebms.client.async.handler.DeliveryTaskHandlerConfig;
import nl.clockwork.ebms.client.async.handler.DeliveryTaskManagerConfig;
import nl.clockwork.ebms.client.sync.DeliveryManagerConfig;
import nl.clockwork.ebms.client.transport.http.EbMSClientConfig;
import nl.clockwork.ebms.common.cache.CacheConfig;
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
import nl.clockwork.ebms.server.config.EbMSServerConfig;
import nl.clockwork.ebms.server.embedded.dao.AdminDAOConfig;
import nl.clockwork.ebms.server.embedded.web.EmbeddedWebConfig;
import nl.clockwork.ebms.server.validation.ValidationConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Configuration
@Import({AdminDAOConfig.class, CertificateMappingConfig.class, CertificateMappingControllerConfig.class, CacheConfig.class, CommonConfig.class, CPAConfig.class,
		CPAControllerConfig.class, DAOConfig.class, DataSourceConfig.class, DeliveryManagerConfig.class, DeliveryTaskManagerConfig.class,
		DeliveryTaskHandlerConfig.class, EbMSClientConfig.class, EbMSControllerConfig.class, nl.clockwork.ebms.server.config.EbMSMessageProcessorConfig.class,
		EbMSServerConfig.class, nl.clockwork.ebms.server.processing.message.EbMSMessageProcessorConfig.class, EmbeddedWebConfig.class, EncryptionConfig.class,
		KeyStoreConfig.class, MessageEventListenerConfig.class, SigningConfig.class, TransactionManagerConfig.class, URLMappingConfig.class,
		URLMappingControllerConfig.class, ValidationConfig.class})
@PropertySource(
		value = {"classpath:nl/clockwork/ebms/default.properties", "classpath:nl/clockwork/ebms/server/default.properties",
				"file:${ebms.configDir}ebms-server.advanced.properties", "file:${ebms.configDir}ebms-server.properties"},
		ignoreResourceNotFound = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmbeddedAppConfig
{
	public static final EbMSPropertySourcesPlaceholderConfigurer PROPERTY_SOURCE = propertySourcesPlaceholderConfigurer();

	EmbeddedAppConfig()
	{
		// do nothing
	}

	private static EbMSPropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer()
	{
		val result = new EbMSPropertySourcesPlaceholderConfigurer();
		val configDir = System.getProperty("ebms.configDir");
		val resources =
				new Resource[]{new ClassPathResource("nl/clockwork/ebms/default.properties"), new ClassPathResource("nl/clockwork/ebms/server/default.properties"),
						new FileSystemResource(configDir + "ebms-server.advanced.properties"), new FileSystemResource(configDir + "ebms-server.properties")};
		result.setLocations(resources);
		result.setIgnoreResourceNotFound(true);
		return result;
	}
}
