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
package nl.clockwork.ebms.delivery.client;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.certificate.CertificateMapper;
import nl.clockwork.ebms.delivery.client.EbMSHttpClientFactory.EbMSHttpClientType;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSClientConfig
{
	@Value("${http.client}")
	EbMSHttpClientType ebMSHttpClientType;
	@Value("${http.connectTimeout}")
	int connectTimeout;
	@Value("${http.readTimeout}")
	int readTimeout;
	@Value("${http.chunkedStreamingMode}")
	boolean chunkedStreamingMode;
	//@Value("${http.base64Writer}")
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

	@Bean
	public EbMSHttpClientFactory ebMSClientFactory()
	{
		return EbMSHttpClientFactory.builder()
				.type(ebMSHttpClientType)
				.connectTimeout(connectTimeout)
				.readTimeout(readTimeout)
				.chunkedStreamingMode(chunkedStreamingMode)
				.base64Writer(base64Writer)
				.proxy(createProxy())
				.enabledProtocols(enabledProtocols)
				.enabledCipherSuites(enabledCipherSuites)
				.verifyHostnames(verifyHostnames)
				.keyStore(clientKeyStore)
				.trustStore(trustStore)
				.httpErrors(createHttpErrors())
				.certificateMapper(certificateMapper)
				.useClientCertificate(useClientCertificate)
				.build();
	}

	private EbMSProxy createProxy()
	{
		return new EbMSProxyFactory(proxyHost,poxyPort,proxyUsername,proxyPassword,nonProxyHosts).getObject();
	}

	private HttpErrors createHttpErrors()
	{
		return new HttpErrors(recoverableInformationalHttpErrors,recoverableRedirectionHttpErrors,recoverableClientHttpErrors,unrecoverableServerHttpErrors);
	}
}
