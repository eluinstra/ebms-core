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
package nl.clockwork.ebms.cli.properties;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 * Writes an {@link AdminProperties} model to a properties file. Keys set in {@link AdminProperties#getAdditionalProperties()} take precedence over the modeled
 * (curated) sections.
 */
public class AdminPropertiesWriter
{
	private final Writer writer;

	public AdminPropertiesWriter(Writer writer)
	{
		this.writer = writer;
	}

	public void write(AdminProperties adminProperties, PropertiesType propertiesType) throws IOException
	{
		val p = new LinkedHashMap<String, String>();
		write(p, adminProperties.getConsoleProperties());
		write(p, adminProperties.getCoreProperties());
		switch (propertiesType)
		{
			case EBMS_ADMIN:
				write(p, adminProperties.getServiceProperties());
				break;
			case EBMS_ADMIN_EMBEDDED:
				write(p, adminProperties.getHttpProperties());
				write(p, adminProperties.getServerDatabase());
				write(p, adminProperties.getSignatureProperties());
				write(p, adminProperties.getEncryptionProperties());
				break;
		}
		write(p, adminProperties.getJdbcProperties());
		// additional (unmodeled) properties take precedence over the curated sections
		val result = new LinkedHashMap<>(p);
		result.putAll(adminProperties.getAdditionalProperties());
		write(result, commentFor(propertiesType));
	}

	private static String commentFor(PropertiesType propertiesType)
	{
		return switch (propertiesType)
		{
			case EBMS_ADMIN -> "EbMS Admin properties";
			case EBMS_ADMIN_EMBEDDED -> "EbMS Admin Embedded properties";
		};
	}

	private void write(Map<String, String> p, ConsoleProperties consoleProperties)
	{
		p.put("maxItemsPerPage", Integer.toString(consoleProperties.getMaxItemsPerPage()));
	}

	private void write(Map<String, String> p, CoreProperties coreProperties)
	{
		p.put("eventListener.type", coreProperties.getEventListener().name());
		p.put("jms.brokerURL", StringUtils.defaultString(coreProperties.getJmsBrokerUrl()));
		p.put("jms.virtualTopics", Boolean.toString(coreProperties.isJmsVirtualTopics()));
		p.put("jms.broker.start", Boolean.toString(coreProperties.isStartEmbeddedBroker()));
		p.put("jms.broker.config", StringUtils.defaultString(coreProperties.getActiveMQConfigFile()));
		p.put("ebmsMessage.deleteContentOnProcessed", Boolean.toString(coreProperties.isDeleteMessageContentOnProcessed()));
	}

	private void write(Map<String, String> p, ServiceProperties serviceProperties)
	{
		p.put("service.ebms.url", StringUtils.defaultString(serviceProperties.getUrl()));
	}

	private void write(Map<String, String> p, HttpProperties httpProperties)
	{
		p.put("ebms.host", httpProperties.getHost());
		p.put("ebms.port", Integer.toString(httpProperties.getPort()));
		p.put("ebms.path", httpProperties.getPath());
		p.put("http.chunkedStreamingMode", Boolean.toString(httpProperties.isChunkedStreamingMode()));
		p.put("http.base64Writer", Boolean.toString(httpProperties.isBase64Writer()));
		p.put("ebms.ssl", Boolean.toString(httpProperties.isSsl()));
		if (httpProperties.isSsl())
			write(p, httpProperties.getSslProperties());
		if (httpProperties.isProxy())
			write(p, httpProperties.getProxyProperties());
	}

	private void write(Map<String, String> p, SslProperties sslProperties)
	{
		if (StringUtils.isNotBlank(sslProperties.getProtocols()))
			p.put("https.protocols", sslProperties.getProtocols());
		if (StringUtils.isNotBlank(sslProperties.getCipherSuites()))
			p.put("https.cipherSuites", sslProperties.getCipherSuites());
		p.put("https.requireClientAuthentication", Boolean.toString(sslProperties.isRequireClientAuthentication()));
		p.put("https.verifyHostnames", Boolean.toString(sslProperties.isVerifyHostnames()));
		write(p, sslProperties.getKeystoreProperties(), "keystore");
		write(p, sslProperties.getClientKeystoreProperties(), "client.keystore");
		write(p, sslProperties.getTruststoreProperties(), "truststore");
	}

	private void write(Map<String, String> p, KeystoreProperties keystoreProperties, String prefix)
	{
		p.put(prefix + ".type", keystoreProperties.getType().name());
		p.put(prefix + ".path", StringUtils.defaultString(keystoreProperties.getUri()));
		p.put(prefix + ".password", StringUtils.defaultString(keystoreProperties.getPassword()));
		p.put(prefix + ".defaultAlias", StringUtils.defaultString(keystoreProperties.getDefaultAlias()));
	}

	private void write(Map<String, String> p, ProxyProperties proxyProperties)
	{
		p.put("http.proxy.host", StringUtils.defaultString(proxyProperties.getHost()));
		if (proxyProperties.getPort() != null)
			p.put("http.proxy.port", Integer.toString(proxyProperties.getPort()));
		p.put("http.proxy.nonProxyHosts", StringUtils.defaultString(proxyProperties.getNonProxyHosts()));
		p.put("http.proxy.username", StringUtils.defaultString(proxyProperties.getUsername()));
		p.put("http.proxy.password", StringUtils.defaultString(proxyProperties.getPassword()));
	}

	private void write(Map<String, String> p, ServerDatabase serverDatabase)
	{
		p.put("database.start", Boolean.toString(serverDatabase.isStart()));
		p.put("database.dir", serverDatabase.getDir());
	}

	private void write(Map<String, String> p, JdbcProperties jdbcProperties)
	{
		p.put("ebms.jdbc.driverClassName", jdbcProperties.getDriverClassName());
		p.put("ebms.jdbc.url", jdbcProperties.getUrl());
		p.put("ebms.jdbc.username", jdbcProperties.getUsername());
		p.put("ebms.jdbc.password", StringUtils.defaultString(jdbcProperties.getPassword()));
	}

	private void write(Map<String, String> p, SignatureProperties signatureProperties)
	{
		if (signatureProperties.isSigning())
			write(p, signatureProperties.getKeystoreProperties(), "signature.keystore");
	}

	private void write(Map<String, String> p, EncryptionProperties encryptionProperties)
	{
		if (encryptionProperties.isEncryption())
			write(p, encryptionProperties.getKeystoreProperties(), "encryption.keystore");
	}

	private void write(Map<String, String> properties, String comment) throws IOException
	{
		val javaProperties = new Properties();
		properties.forEach(javaProperties::setProperty);
		javaProperties.store(writer, comment);
		writer.flush();
	}
}
