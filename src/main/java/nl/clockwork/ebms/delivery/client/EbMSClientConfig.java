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
import javax.net.ssl.SSLParameters;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.certificate.CertificateMapper;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSClientConfig
{
	@Value("${http.connectTimeout}")
	int connectTimeout;
	@Value("${http.readTimeout}")
	int readTimeout;
	@Value("${http.proxy.host}")
	String proxyHost;
	@Value("${http.proxy.port}")
	int poxyPort;
	@Value("${http.proxy.nonProxyHosts}")
	Set<String> nonProxyHosts;
	@Value("${http.proxy.username}")
	String proxyUsername;
	@Value("${http.proxy.password}")
	String proxyPassword;
	@Value("${https.protocols}")
	String[] enabledProtocols;
	@Value("${https.cipherSuites}")
	String[] enabledCipherSuites;
	@Value("${https.verifyHostnames}")
	boolean verifyHostnames;
	@Value("${http.errors.informational.recoverable}")
	String recoverableInformationalHttpErrors;
	@Value("${http.errors.redirection.recoverable}")
	String recoverableRedirectionHttpErrors;
	@Value("${http.errors.client.recoverable}")
	String recoverableClientHttpErrors;
	@Value("${http.errors.server.unrecoverable}")
	String unrecoverableServerHttpErrors;
	@Value("${https.useClientCertificate}")
	boolean useClientCertificate;

	@Bean
	@DependsOn("ebMSProxyFactory")
	public EbMSHttpClientFactory ebMSClientFactory(
			EbMSProxy ebMSProxy,
			SSLParameters sslParameters,
			@Qualifier("clientKeyStore") EbMSKeyStore clientKeyStore,
			EbMSTrustStore trustStore,
			CertificateMapper certificateMapper)
	{
		return EbMSHttpClientFactory.builder()
				.connectTimeout(connectTimeout)
				.readTimeout(readTimeout)
				.proxy(ebMSProxy)
				.sslParameters(sslParameters)
				.verifyHostnames(verifyHostnames)
				.keyStore(clientKeyStore)
				.trustStore(trustStore)
				.httpErrors(httpErrors())
				.certificateMapper(certificateMapper)
				.useClientCertificate(useClientCertificate)
				.build();
	}

	private HttpErrors httpErrors()
	{
		return new HttpErrors(recoverableInformationalHttpErrors, recoverableRedirectionHttpErrors, recoverableClientHttpErrors, unrecoverableServerHttpErrors);
	}

	@Bean
	public EbMSProxyFactory ebMSProxyFactory()
	{
		return new EbMSProxyFactory(proxyHost, poxyPort, proxyUsername, proxyPassword, nonProxyHosts);
	}

	@Bean
	public SSLParametersFactory sslParametersFactory()
	{
		return new SSLParametersFactory(enabledProtocols, enabledCipherSuites);
	}
}
