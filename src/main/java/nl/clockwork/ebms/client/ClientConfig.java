/**
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
package nl.clockwork.ebms.client;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSThreadPoolExecutor;
import nl.clockwork.ebms.client.DeliveryManagerFactory.DeliveryManagerType;
import nl.clockwork.ebms.client.EbMSHttpClientFactory.EbMSHttpClientType;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CertificateMapper;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientConfig
{
	@Value("${http.client}")
	EbMSHttpClientType ebMSHttpClientType;
	@Value("${http.connectTimeout}")
	int connectTimeout;
	@Value("${http.chunkedStreamingMode}")
	boolean chunkedStreamingMode;
	@Value("${http.base64Writer}")
	boolean base64Writer;
	@Value("${http.proxy.host}")
	String proxyHost;
	@Value("${http.proxy.port}")
	int poxyPort;
	@Value("${http.proxy.nonProxyHosts}")
	String proxyUsername;
	@Value("${http.proxy.username}")
	String proxyPassword;
	@Value("${http.proxy.password}")
	Set<String> nonProxyHosts;
	@Value("${https.protocols}")
	String[] enabledProtocols;
	@Value("${https.cipherSuites}")
	String[] enabledCipherSuites;
	@Value("${https.verifyHostnames}")
	boolean verifyHostnames;
	@Autowired
	@Qualifier("clientKeyStore")
	EbMSKeyStore clientKeyStore;
	@Autowired
	EbMSTrustStore trustStore;
	@Value("${http.errors.informational.recoverable}")
	String recoverableInformationalHttpErrors;
	@Value("${http.errors.redirection.recoverable}")
	String recoverableRedirectionHttpErrors;
	@Value("${http.errors.client.recoverable}")
	String recoverableClientHttpErrors;
	@Value("${http.errors.server.unrecoverable}")
	String unrecoverableServerHttpErrors;
	@Autowired
	CertificateMapper certificateMapper;
	@Value("${https.useClientCertificate}")
	boolean useClientCertificate;
	@Value("${deliveryManager.type}")
	DeliveryManagerType deliveryManagerType;
	@Value("${deliveryManager.minThreads}")
	Integer minThreads;
	@Value("${deliveryManager.maxThreads}")
	Integer maxThreads;
	@Value("${messageQueue.maxEntries}")
	int maxEntries;
	@Value("${messageQueue.timeout}")
	int timeout;
	@Autowired
	CPAManager cpaManager;
	@Autowired
	JmsTemplate jmsTemplate;

	@Bean
	public EbMSHttpClientFactory ebMSClientFactory() throws Exception
	{
		val ebMSProxy = new EbMSProxyFactory(proxyHost,poxyPort,proxyUsername,proxyPassword,nonProxyHosts).getObject();
		val httpErrors = new HttpErrors(recoverableInformationalHttpErrors,recoverableRedirectionHttpErrors,recoverableClientHttpErrors,unrecoverableServerHttpErrors);
		return EbMSHttpClientFactory.builder()
				.type(ebMSHttpClientType)
				.connectTimeout(connectTimeout)
				.chunkedStreamingMode(chunkedStreamingMode)
				.base64Writer(base64Writer)
				.proxy(ebMSProxy)
				.enabledProtocols(enabledProtocols)
				.enabledCipherSuites(enabledCipherSuites)
				.verifyHostnames(verifyHostnames)
				.keyStore(clientKeyStore)
				.trustStore(trustStore)
				.httpErrors(httpErrors)
				.certificateMapper(certificateMapper)
				.useClientCertificate(useClientCertificate)
				.build();
	}

	@Bean
	public DeliveryManager deliveryManager() throws Exception
	{
		return DeliveryManagerFactory.builder()
				.setType(deliveryManagerType)
				.setEbMSThreadPoolExecutor(new EbMSThreadPoolExecutor(minThreads,maxThreads))
				.setMessageQueue(new EbMSMessageQueue(maxEntries,timeout))
				.setCpaManager(cpaManager)
				.setEbMSClientFactory(ebMSClientFactory())
				.setJmsTemplate(jmsTemplate)
				.build()
				.getObject();
	}
}
