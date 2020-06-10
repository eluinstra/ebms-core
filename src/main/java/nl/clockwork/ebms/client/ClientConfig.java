package nl.clockwork.ebms.client;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

@Configuration(proxyBeanMethods = false)
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
	@Value("${eventProcessor.maxTreads}")
	Integer maxThreads;
	@Value("${messageQueue.maxEntries}")
	int maxEntries;
	@Value("${messageQueue.timeout}")
	int timeout;
	@Autowired
	CPAManager cpaManager;
	@Value("${jms.brokerURL}")
	String jmsBrokerURL;

	@Bean
	public EbMSHttpClientFactory ebMSClientFactory() throws Exception
	{
		val ebMSProxy = new EbMSProxyFactory(proxyHost,poxyPort,proxyUsername,proxyPassword,nonProxyHosts).getObject();
		val httpErrors = new HttpErrors(recoverableInformationalHttpErrors,recoverableRedirectionHttpErrors,recoverableClientHttpErrors,unrecoverableServerHttpErrors);
		return EbMSHttpClientFactory.builder()
				.setType(ebMSHttpClientType)
				.setConnectTimeout(connectTimeout)
				.setChunkedStreamingMode(chunkedStreamingMode)
				.setBase64Writer(base64Writer)
				.setProxy(ebMSProxy)
				.setEnabledProtocols(enabledProtocols)
				.setEnabledCipherSuites(enabledCipherSuites)
				.setVerifyHostnames(verifyHostnames)
				.setKeyStore(clientKeyStore)
				.setTrustStore(trustStore)
				.setHttpErrors(httpErrors)
				.setCertificateMapper(certificateMapper)
				.setUseClientCertificate(useClientCertificate)
				.build();
	}

	@Bean
	public DeliveryManager deliveryManager() throws Exception
	{
		return DeliveryManagerFactory.builder()
				.setType(deliveryManagerType)
				.setEbMSThreadPoolExecutor(new EbMSThreadPoolExecutor(maxThreads))
				.setMessageQueue(new EbMSMessageQueue(maxEntries,timeout))
				.setCpaManager(cpaManager)
				.setEbMSClientFactory(ebMSClientFactory())
				.setJmsBrokerURL(jmsBrokerURL)
				.build()
				.getObject();
	}
}
