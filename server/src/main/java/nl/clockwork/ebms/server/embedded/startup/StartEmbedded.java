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
package nl.clockwork.ebms.server.embedded.startup;

import jakarta.servlet.DispatcherType;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import lombok.val;
import nl.clockwork.ebms.common.security.KeyStoreType;
import nl.clockwork.ebms.common.util.LoggingUtils;
import nl.clockwork.ebms.server.embedded.config.CustomErrorHandler;
import nl.clockwork.ebms.server.embedded.config.EbMSKeyStore;
import nl.clockwork.ebms.server.embedded.config.EmbeddedAppConfig;
import nl.clockwork.ebms.server.embedded.utils.Utils;
import nl.clockwork.ebms.server.endpoint.servlet.EbMSServlet;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class StartEmbedded extends Start
{
	private static final String DELIVERY_TASK_HANDLER_START_PROPERTY = "deliveryTaskHandler.start";
	private static final String EBMS_HOST_PROPERTY = "ebms.host";
	private static final String EBMS_PORT_PROPERTY = "ebms.port";
	private static final String EBMS_PATH_PROPERTY = "ebms.path";
	private static final String EBMS_CONNECTION_LIMIT_PROPERTY = "ebms.connectionLimit";
	private static final String EBMS_QUERIES_PER_SECOND_PROPERTY = "ebms.queriesPerSecond";
	private static final String EBMS_USER_QUERIES_PER_SECOND_PROPERTY = "ebms.userQueriesPerSecond";
	private static final String EBMS_SSL_PROPERTY = "ebms.ssl";
	private static final String EBMS_VERIFY_HOSTNAMES_PROPERTY = "ebms.verifyHostnames";
	private static final String EBMS_ECHO_HEADER_NAMES_PROPERTY = "ebms.echoHeaderNames";
	private static final String HTTPS_PROTOCOLS_PROPERTY = "https.protocols";
	private static final String HTTPS_CIPHER_SUITES_PROPERTY = "https.cipherSuites";
	private static final String HTTPS_REQUIRE_CLIENT_AUTHENTICATION_PROPERTY = "https.requireClientAuthentication";
	private static final String HTTPS_CLIENT_CERTIFICATE_HEADER_PROPERTY = "https.clientCertificateHeader";
	private static final String HTTPS_CLIENT_CERTIFICATE_AUTHENTICATION_PROPERTY = "https.clientCertificateAuthentication";
	private static final String KEYSTORE_TYPE_PROPERTY = "keystore.type";
	private static final String KEYSTORE_PATH_PROPERTY = "keystore.path";
	private static final String KEYSTORE_PASSWORD_PROPERTY = "keystore.password";
	private static final String KEYSTORE_DEFAULT_ALIAS_PROPERTY = "keystore.defaultAlias";
	private static final String TRUSTSTORE_TYPE_PROPERTY = "truststore.type";
	private static final String TRUSTSTORE_PATH_PROPERTY = "truststore.path";
	private static final String TRUSTSTORE_PASSWORD_PROPERTY = "truststore.password";
	private static final String LOGGING_MDC_PROPERTY = "logging.mdc";
	private static final String LOGGING_MDC_AUDIT_PROPERTY = "logging.mdc.audit";
	private static final String LOGGING_MDC_HEADER_NAMES_PROPERTY = "logging.mdc.headerNames";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private static final String DEFAULT_EBMS_PORT = "8888";
	private static final String EBMS_CONNECTOR_NAME = "ebms";

	public static void main(String[] args) throws Exception
	{
		LogUtils.setLoggerClass(org.apache.cxf.common.logging.Slf4jLogger.class);
		val app = new StartEmbedded();
		app.startEmbeddedService(args);
	}

	@SuppressWarnings("java:S2221")
	private void startEmbeddedService(String[] args) throws Exception
	{
		val options = createOptions();
		if (containsHelpOption(options, args))
			printUsage(options);
		setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", "*");
		if (isEbmsClientDisabled())
			setProperty(DELIVERY_TASK_HANDLER_START_PROPERTY, FALSE);
		init();
		server.setHandler(handlerCollection);
		server.addBean(new CustomErrorHandler());
		val properties = getProperties();
		if (isJmxEnabled())
			initJMX(server);
		if (isSoapEnabled() || isHealthEnabled() || !isHeadless() || !isEbmsServerDisabled())
			try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext())
			{
				context.scan("nl.clockwork.ebms");
				getPluginConfigClasses().forEach(context::register);
				getConfigClasses().forEach(context::register);
				val contextLoaderListener = new ContextLoaderListener(context);
				if (isSoapEnabled() || !isHeadless())
				{
					initWebServer();
					handlerCollection.addHandler(createWebContextHandler(contextLoaderListener));
				}
				if (isHealthEnabled())
				{
					initHealthServer();
					handlerCollection.addHandler(createHealthContextHandler());
				}
				if (!isEbmsServerDisabled())
				{
					initEbMSServer(properties, server);
					handlerCollection.addHandler(createEbMSContextHandler(properties, contextLoaderListener));
				}
				println("Starting Server...");
				try
				{
					server.start();
				}
				catch (Exception e)
				{
					e.printStackTrace();
					server.stop();
					exit(1);
				}
				println("Server started.");
				server.join();
			}
		else
			try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
			{
				context.register(EmbeddedAppConfig.class);
				getPluginConfigClasses().forEach(context::register);
				getConfigClasses().forEach(context::register);
				println("Starting Server...");
				context.refresh();
				context.start();
				println("Server started.");
				Thread.currentThread().join();
			}
	}

	private Properties getProperties() throws IOException
	{
		return EmbeddedAppConfig.PROPERTY_SOURCE.getProperties();
	}

	private void initEbMSServer(Properties properties, Server server) throws GeneralSecurityException, IOException
	{
		val connector =
				isEbmsSslEnabled(properties) ? createEbMSHttpsConnector(properties, createEbMSSslContextFactory(properties)) : createEbMSHttpConnector(properties);
		server.addConnector(connector);
		val connectionLimit = properties.getProperty(EBMS_CONNECTION_LIMIT_PROPERTY);
		if (StringUtils.isNotEmpty(connectionLimit))
			addConnectionLimit(server, connector, Integer.parseInt(connectionLimit));
	}

	private ServerConnector createEbMSHttpConnector(Properties properties)
	{
		val httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		val result = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
		result.setHost(StringUtils.isEmpty(properties.getProperty(EBMS_HOST_PROPERTY)) ? DEFAULT_HOST : properties.getProperty(EBMS_HOST_PROPERTY));
		result.setPort(
				Integer.parseInt(StringUtils.isEmpty(properties.getProperty(EBMS_PORT_PROPERTY)) ? DEFAULT_EBMS_PORT : properties.getProperty(EBMS_PORT_PROPERTY)));
		result.setName(EBMS_CONNECTOR_NAME);
		println("EbMS Service configured on http://" + Utils.getHost(result.getHost()) + ":" + result.getPort() + properties.getProperty(EBMS_PATH_PROPERTY));
		return result;
	}

	private SslContextFactory.Server createEbMSSslContextFactory(Properties properties) throws GeneralSecurityException, IOException
	{
		val result = new SslContextFactory.Server();
		val ebMSKeyStore = EbMSKeyStore.of(
				KeyStoreType.valueOf(properties.getProperty(KEYSTORE_TYPE_PROPERTY)),
				properties.getProperty(KEYSTORE_PATH_PROPERTY),
				properties.getProperty(KEYSTORE_PASSWORD_PROPERTY),
				properties.getProperty(KEYSTORE_DEFAULT_ALIAS_PROPERTY));
		addEbMSKeyStore(properties, result, ebMSKeyStore);
		if (isEbmsClientAuthenticationEnabled(properties))
			addEbMSTrustStore(properties, result);
		return result;
	}

	private void addEbMSKeyStore(Properties properties, SslContextFactory.Server sslContextFactory, EbMSKeyStore ebMSKeyStore)
	{
		if (!StringUtils.isEmpty(properties.getProperty(HTTPS_PROTOCOLS_PROPERTY)))
			sslContextFactory.setIncludeProtocols(StringUtils.stripAll(StringUtils.split(properties.getProperty(HTTPS_PROTOCOLS_PROPERTY), ',')));
		if (!StringUtils.isEmpty(properties.getProperty(HTTPS_CIPHER_SUITES_PROPERTY)))
			sslContextFactory.setIncludeCipherSuites(StringUtils.stripAll(StringUtils.split(properties.getProperty(HTTPS_CIPHER_SUITES_PROPERTY), ',')));
		sslContextFactory.setKeyStore(ebMSKeyStore.getKeyStore());
		sslContextFactory.setKeyStorePassword(ebMSKeyStore.getPassword());
		if (StringUtils.isNotEmpty(ebMSKeyStore.getDefaultAlias()))
			sslContextFactory.setCertAlias(ebMSKeyStore.getDefaultAlias());
	}

	private void addEbMSTrustStore(Properties properties, SslContextFactory.Server sslContextFactory) throws IOException
	{
		val trustStore = getResource(properties.getProperty(TRUSTSTORE_PATH_PROPERTY));
		if (trustStore != null && trustStore.exists())
		{
			sslContextFactory.setNeedClientAuth(true);
			sslContextFactory.setTrustStoreType(properties.getProperty(TRUSTSTORE_TYPE_PROPERTY));
			sslContextFactory.setTrustStoreResource(trustStore);
			sslContextFactory.setTrustStorePassword(properties.getProperty(TRUSTSTORE_PASSWORD_PROPERTY));
		}
		else
		{
			println("EbMS Service not available: trustStore " + properties.getProperty(TRUSTSTORE_PATH_PROPERTY) + " not found!");
			exit(1);
		}
	}

	private ServerConnector createEbMSHttpsConnector(Properties properties, SslContextFactory.Server sslContextFactory)
	{
		val httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		httpConfig.addCustomizer(new SecureRequestCustomizer(isEbmsHostnameVerificationEnabled(properties)));
		val result = new ServerConnector(server, sslContextFactory, new HttpConnectionFactory(httpConfig));
		result.setHost(StringUtils.isEmpty(properties.getProperty(EBMS_HOST_PROPERTY)) ? DEFAULT_HOST : properties.getProperty(EBMS_HOST_PROPERTY));
		result.setPort(
				Integer.parseInt(StringUtils.isEmpty(properties.getProperty(EBMS_PORT_PROPERTY)) ? DEFAULT_EBMS_PORT : properties.getProperty(EBMS_PORT_PROPERTY)));
		result.setName(EBMS_CONNECTOR_NAME);
		println("EbMS Service configured on https://" + Utils.getHost(result.getHost()) + ":" + result.getPort() + properties.getProperty(EBMS_PATH_PROPERTY));
		return result;
	}

	protected ServletContextHandler createEbMSContextHandler(Properties properties, ContextLoaderListener contextLoaderListener)
			throws IOException, NoSuchAlgorithmException
	{
		val result = new ServletContextHandler();
		result.setVirtualHosts(List.of("@" + EBMS_CONNECTOR_NAME));
		result.setContextPath("/");
		if (StringUtils.isNotEmpty(properties.getProperty(EBMS_ECHO_HEADER_NAMES_PROPERTY)))
			result.addFilter(createEchoServletFilterHolder(properties.getProperty(EBMS_ECHO_HEADER_NAMES_PROPERTY)), "/*", EnumSet.allOf(DispatcherType.class));
		if (StringUtils.isNotEmpty(properties.getProperty(LOGGING_MDC_HEADER_NAMES_PROPERTY)))
			result.addFilter(createMDCServletFilterHolder(properties.getProperty(LOGGING_MDC_HEADER_NAMES_PROPERTY)), "/*", EnumSet.allOf(DispatcherType.class));
		if (isEbmsMdcAuditEnabled(properties))
			result.addFilter(createRemoteAddressMDCFilterHolder(), "/*", EnumSet.allOf(DispatcherType.class));
		if (!StringUtils.isEmpty(properties.getProperty(EBMS_QUERIES_PER_SECOND_PROPERTY)))
			result.addFilter(createRateLimiterFilterHolder(properties.getProperty(EBMS_QUERIES_PER_SECOND_PROPERTY)), "/*", EnumSet.allOf(DispatcherType.class));
		if (!StringUtils.isEmpty(properties.getProperty(EBMS_USER_QUERIES_PER_SECOND_PROPERTY)))
			result.addFilter(
					createUserRateLimiterFilterHolder(properties.getProperty(EBMS_USER_QUERIES_PER_SECOND_PROPERTY)),
					"/*",
					EnumSet.allOf(DispatcherType.class));
		if (isEbmsClientCertificateAuthenticationEnabled(properties))
			result.addFilter(
					createClientCertificateManagerFilterHolder(properties.getProperty(HTTPS_CLIENT_CERTIFICATE_HEADER_PROPERTY)),
					"/*",
					EnumSet.allOf(DispatcherType.class));
		result.addServlet(EbMSServlet.class, properties.getProperty(EBMS_PATH_PROPERTY));
		result.addEventListener(contextLoaderListener);
		return result;
	}

	private boolean isEbmsSslEnabled(Properties properties)
	{
		return TRUE.equals(properties.getProperty(EBMS_SSL_PROPERTY));
	}

	private boolean isEbmsClientAuthenticationEnabled(Properties properties)
	{
		return TRUE.equals(properties.getProperty(HTTPS_REQUIRE_CLIENT_AUTHENTICATION_PROPERTY));
	}

	private boolean isEbmsHostnameVerificationEnabled(Properties properties)
	{
		return TRUE.equals(properties.getProperty(EBMS_VERIFY_HOSTNAMES_PROPERTY));
	}

	private boolean isEbmsMdcAuditEnabled(Properties properties)
	{
		return LoggingUtils.Status.ENABLED.name().equals(properties.getProperty(LOGGING_MDC_PROPERTY))
				&& LoggingUtils.Status.ENABLED.name().equals(properties.getProperty(LOGGING_MDC_AUDIT_PROPERTY));
	}

	private boolean isEbmsClientCertificateAuthenticationEnabled(Properties properties)
	{
		return TRUE.equalsIgnoreCase(properties.getProperty(HTTPS_CLIENT_CERTIFICATE_AUTHENTICATION_PROPERTY));
	}

	private boolean isEbmsServerDisabled() throws IOException
	{
		val properties = getProperties();
		return Boolean.parseBoolean(properties.getProperty("ebms.server.disabled", "false"));
	}

	private boolean isEbmsClientDisabled() throws IOException
	{
		val properties = getProperties();
		return Boolean.parseBoolean(properties.getProperty("ebms.client.disabled", "false"));
	}

	protected Options createOptions()
	{
		val result = new Options();
		result.addOption(HELP_OPTION, false, "print this message");
		return result;
	}

	protected boolean isJmxEnabled()
	{
		return getBooleanProperty("api.jmx.enabled", false);
	}

	protected boolean isSoapEnabled()
	{
		return getBooleanProperty("api.soap.enabled", true);
	}

	protected boolean isHealthEnabled()
	{
		return getBooleanProperty("api.health.enabled", false);
	}

	protected boolean isHeadless()
	{
		return getBooleanProperty("api.headless", false);
	}

	private boolean containsHelpOption(Options options, String[] args)
	{
		try
		{
			val cmd = new org.apache.commons.cli.DefaultParser().parse(options, args);
			return cmd.hasOption(HELP_OPTION);
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
