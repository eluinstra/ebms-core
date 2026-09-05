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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.common.event.MessageEventListenerConfig.EventListenerType;
import nl.clockwork.ebms.common.security.KeyStoreType;
import org.apache.commons.lang3.StringUtils;

/**
 * Reads an ebms-admin properties file into an {@link AdminProperties} model. Missing keys fall back to the defaults of the model; keys that are not modeled are
 * collected in {@link AdminProperties#getAdditionalProperties()}.
 */
@Slf4j
public class AdminPropertiesReader
{
	private final Properties properties;
	private final PropertiesType propertiesType;
	private final List<String> knownKeys = new ArrayList<>();

	public AdminPropertiesReader(Properties properties, PropertiesType propertiesType)
	{
		this.properties = properties;
		this.propertiesType = propertiesType;
	}

	public AdminPropertiesReader(InputStream inputStream, PropertiesType propertiesType) throws IOException
	{
		this.properties = new Properties();
		properties.load(inputStream);
		this.propertiesType = propertiesType;
	}

	public AdminProperties read()
	{
		val result = new AdminProperties();
		read(result.getConsoleProperties());
		read(result.getCoreProperties());
		switch (propertiesType)
		{
			case EBMS_ADMIN:
				read(result.getServiceProperties());
				break;
			case EBMS_ADMIN_EMBEDDED:
				read(result.getHttpProperties());
				read(result.getServerDatabase());
				break;
		}
		read(result.getJdbcProperties());
		if (PropertiesType.EBMS_ADMIN_EMBEDDED.equals(propertiesType))
		{
			read(result.getSignatureProperties());
			read(result.getEncryptionProperties());
		}
		readAdditional(result);
		return result;
	}

	private void known(String key)
	{
		knownKeys.add(key);
	}

	private String get(String key, String defaultValue)
	{
		known(key);
		return StringUtils.defaultIfEmpty(properties.getProperty(key), defaultValue);
	}

	private int getInt(String key, int defaultValue)
	{
		known(key);
		return Integer.parseInt(get(key, Integer.toString(defaultValue)));
	}

	private boolean getBool(String key, boolean defaultValue)
	{
		known(key);
		return Boolean.parseBoolean(get(key, Boolean.toString(defaultValue)));
	}

	private List<String> getList(String key)
	{
		known(key);
		val value = properties.getProperty(key);
		val result = new ArrayList<String>();
		if (StringUtils.isNotBlank(value))
			result.addAll(List.of(value.split(",")));
		return result.stream().map(String::trim).toList();
	}

	private void read(ConsoleProperties consoleProperties)
	{
		consoleProperties.setMaxItemsPerPage(getInt("maxItemsPerPage", consoleProperties.getMaxItemsPerPage()));
	}

	private void read(CoreProperties coreProperties)
	{
		known("eventListener.type");
		val eventListener = properties.getProperty("eventListener.type");
		if (StringUtils.isNotBlank(eventListener))
		{
			try
			{
				coreProperties.setEventListener(EventListenerType.valueOf(eventListener.trim()));
			}
			catch (IllegalArgumentException e)
			{
				log.warn("Unknown eventListener.type '{}', using default", eventListener);
			}
		}
		coreProperties.setJmsBrokerUrl(get("jms.brokerURL", coreProperties.getJmsBrokerUrl()));
		coreProperties.setJmsVirtualTopics(getBool("jms.virtualTopics", coreProperties.isJmsVirtualTopics()));
		coreProperties.setStartEmbeddedBroker(getBool("jms.broker.start", coreProperties.isStartEmbeddedBroker()));
		coreProperties.setActiveMQConfigFile(get("jms.broker.config", coreProperties.getActiveMQConfigFile()));
		coreProperties.setDeleteMessageContentOnProcessed(getBool("ebmsMessage.deleteContentOnProcessed", coreProperties.isDeleteMessageContentOnProcessed()));
	}

	private void read(ServiceProperties serviceProperties)
	{
		serviceProperties.setUrl(get("service.ebms.url", serviceProperties.getUrl()));
	}

	private void read(HttpProperties httpProperties)
	{
		httpProperties.setHost(get("ebms.host", httpProperties.getHost()));
		httpProperties.setPort(getInt("ebms.port", httpProperties.getPort()));
		httpProperties.setPath(get("ebms.path", httpProperties.getPath()));
		httpProperties.setChunkedStreamingMode(getBool("http.chunkedStreamingMode", httpProperties.isChunkedStreamingMode()));
		httpProperties.setBase64Writer(getBool("http.base64Writer", httpProperties.isBase64Writer()));
		httpProperties.setSsl(getBool("ebms.ssl", httpProperties.isSsl()));
		if (httpProperties.isSsl())
			read(httpProperties.getSslProperties());
		known("http.proxy.host");
		httpProperties.setProxy(StringUtils.isNotBlank(properties.getProperty("http.proxy.host")));
		if (httpProperties.isProxy())
			read(httpProperties.getProxyProperties());
	}

	private void read(SslProperties sslProperties)
	{
		sslProperties.setEnabledProtocols(getList("https.protocols"));
		sslProperties.setEnabledCipherSuites(getList("https.cipherSuites"));
		sslProperties.setRequireClientAuthentication(getBool("https.requireClientAuthentication", sslProperties.isRequireClientAuthentication()));
		sslProperties.setVerifyHostnames(getBool("https.verifyHostnames", sslProperties.isVerifyHostnames()));
		read(sslProperties.getKeystoreProperties(), "keystore");
		read(sslProperties.getClientKeystoreProperties(), "client.keystore");
		read(sslProperties.getTruststoreProperties(), "truststore");
	}

	private void read(KeystoreProperties keystoreProperties, String prefix)
	{
		known(prefix + ".type");
		val type = properties.getProperty(prefix + ".type", keystoreProperties.getType().name());
		try
		{
			keystoreProperties.setType(KeyStoreType.valueOf(type.trim().toUpperCase()));
		}
		catch (IllegalArgumentException e)
		{
			log.warn("Unknown {} type '{}', using default", prefix, type);
		}
		keystoreProperties.setUri(get(prefix + ".path", keystoreProperties.getUri()));
		keystoreProperties.setPassword(get(prefix + ".password", keystoreProperties.getPassword()));
		keystoreProperties.setDefaultAlias(get(prefix + ".defaultAlias", keystoreProperties.getDefaultAlias()));
	}

	private void read(ProxyProperties proxyProperties)
	{
		proxyProperties.setHost(get("http.proxy.host", proxyProperties.getHost()));
		known("http.proxy.port");
		val port = properties.getProperty("http.proxy.port");
		proxyProperties.setPort(StringUtils.isBlank(port) ? proxyProperties.getPort() : Integer.parseInt(port));
		proxyProperties.setNonProxyHosts(get("http.proxy.nonProxyHosts", proxyProperties.getNonProxyHosts()));
		proxyProperties.setUsername(get("http.proxy.username", proxyProperties.getUsername()));
		proxyProperties.setPassword(get("http.proxy.password", proxyProperties.getPassword()));
	}

	private void read(ServerDatabase serverDatabase)
	{
		serverDatabase.setStart(getBool("database.start", serverDatabase.isStart()));
		serverDatabase.setDir(get("database.dir", serverDatabase.getDir()));
	}

	private void read(SignatureProperties signatureProperties)
	{
		signatureProperties.setSigning(hasKeystore("signature.keystore"));
		if (signatureProperties.isSigning())
			read(signatureProperties.getKeystoreProperties(), "signature.keystore");
	}

	private void read(EncryptionProperties encryptionProperties)
	{
		encryptionProperties.setEncryption(hasKeystore("encryption.keystore"));
		if (encryptionProperties.isEncryption())
			read(encryptionProperties.getKeystoreProperties(), "encryption.keystore");
	}

	/**
	 * The core has no boolean to switch signing/encryption on and off: it reads the keystore keys directly. A section is therefore considered active when any of
	 * its keys is present.
	 */
	private boolean hasKeystore(String prefix)
	{
		for (String suffix : new String[]{".path", ".type", ".password", ".defaultAlias"})
		{
			String value = properties.getProperty(prefix + suffix);
			if (StringUtils.isNotBlank(value))
			{
				known(prefix + suffix);
				return true;
			}
		}
		return false;
	}

	private void read(JdbcProperties jdbcProperties)
	{
		known("ebms.jdbc.driverClassName");
		val driverClassName = properties.getProperty("ebms.jdbc.driverClassName");
		if (StringUtils.isNotBlank(driverClassName))
		{
			JdbcDriver.getJdbcDriver(driverClassName)
					.ifPresentOrElse(jdbcProperties::setDriver, () -> log.warn("Unknown ebms.jdbc.driverClassName '{}', using default", driverClassName));
		}
		known("ebms.jdbc.url");
		val url = properties.getProperty("ebms.jdbc.url");
		JdbcUrl.parse(url).ifPresentOrElse(parsed ->
		{
			jdbcProperties.setJdbcUrl(parsed);
		}, () -> log.warn("Cannot parse ebms.jdbc.url '{}', using default", url));
		jdbcProperties.setUsername(get("ebms.jdbc.username", jdbcProperties.getUsername()));
		jdbcProperties.setPassword(get("ebms.jdbc.password", jdbcProperties.getPassword()));
	}

	private void readAdditional(AdminProperties result)
	{
		val additional = new LinkedHashMap<String, String>();
		for (val name : properties.stringPropertyNames())
		{
			if (!knownKeys.contains(name))
				additional.put(name, properties.getProperty(name));
		}
		result.getAdditionalProperties().putAll(additional);
	}
}
