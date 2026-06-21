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
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.remote.JMXServiceURL;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.PluginProvider;
import nl.clockwork.ebms.common.security.KeyStoreType;
import nl.clockwork.ebms.server.embedded.config.EbMSKeyStore;
import nl.clockwork.ebms.server.embedded.utils.Utils;
import nl.clockwork.ebms.server.embedded.web.ExtensionProvider;
import nl.clockwork.ebms.server.endpoint.servlet.filters.HealthServlet;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.jmx.ConnectorServer;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.Constraint.Authorization;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jline.prompt.InputResult;
import org.jline.prompt.Prompter;
import org.jline.prompt.PrompterFactory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@RequiredArgsConstructor
@SuppressWarnings("removal")
public class Start implements SystemInterface
{
	protected static final String HELP_OPTION = "h";
	protected static final String DEFAULT_HOST = "0.0.0.0";
	private static final String DEFAULT_PORT = "8080";
	private static final String DEFAULT_SSL_PORT = "8443";
	private static final String DEFAULT_PATH = "/";
	private static final String DEFAULT_HEALTH_PORT = "8008";
	private static final String DEFAULT_JMS_PORT = "1999";
	private static final String DEFAULT_KEYSTORE_TYPE = KeyStoreType.PKCS12.name();
	private static final String DEFAULT_KEYSTORE_FILE = "nl/clockwork/ebms/keystore.p12";
	private static final int MIN_PASSWORD_LENGTH = 12;
	private static final String PASSWORD_PROMPT = "password";
	private static final String DEFAULT_CONFIG_DIR = "";
	private static final String WEB_CONNECTOR_NAME = "web";
	private static final String HEALTH_CONNECTOR_NAME = "health";
	private static final String SOAP_URL = "/service";
	private static final String HEALTH_URL = "/health";
	private static final String NONE = "<none>";
	private static final String REALM = "Realm";
	private static final String REALM_FILE = "realm.properties";

	Server server = new Server();
	Handler.Sequence handlerCollection = new Handler.Sequence();
	Terminal terminal = createTerminal();
	Prompter prompter = PrompterFactory.create(terminal);

	public static void main(String[] args) throws Exception
	{
		LogUtils.setLoggerClass(org.apache.cxf.common.logging.Slf4jLogger.class);
		val app = new Start();
		app.startService(args);
	}

	@SuppressWarnings("java:S2221")
	private void startService(String[] args) throws Exception
	{
		val options = createOptions();
		if (containsHelpOption(options, args))
		{
			printUsage(options);
			return;
		}
		init();
		server.setHandler(handlerCollection);
		if (isJmxEnabled())
			initJMX(server);
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
			println("Starting Server...");
			try
			{
				server.start();
			}
			catch (Exception e)
			{
				server.stop();
				exit(1);
			}
			println("Server started.");
			server.join();
		}
	}

	protected Options createOptions()
	{
		val result = new Options();
		result.addOption(HELP_OPTION, false, "print this message");
		return result;
	}

	protected void printUsage(Options options)
	{
		println("Usage: " + getClass().getSimpleName());
		println("All configuration is done via properties in default.properties or custom properties files.");
		println("See documentation for available properties.");
		exit(0);
	}

	protected List<Class<?>> getConfigClasses()
	{
		return new ArrayList<>(
				ExtensionProvider.get().stream().filter(p -> p.getSpringConfigurationClass() != null).map(p -> (Class<?>)p.getSpringConfigurationClass()).toList());
	}

	protected List<Class<?>> getPluginConfigClasses()
	{
		return new ArrayList<>(
				PluginProvider.get().stream().filter(p -> p.getSpringConfigurationClass() != null).map(p -> (Class<?>)p.getSpringConfigurationClass()).toList());
	}

	protected void init()
	{
		val configDir = getProperty("api.configDir", DEFAULT_CONFIG_DIR);
		setProperty("ebms.configDir", configDir);
		println("Using config directory: " + configDir);
	}

	protected void initWebServer() throws GeneralSecurityException, IOException
	{
		val connector = isSslEnabled() ? createHttpsConnector(createSslContextFactory()) : createHttpConnector();
		server.addConnector(connector);
		if (hasConnectionLimit())
			addConnectionLimit(server, connector, getConnectionLimit());
	}

	protected void addConnectionLimit(Server targetServer, ServerConnector connector, int connectionLimit)
	{
		targetServer.addBean(new ConnectionLimit(connectionLimit, connector));
	}

	private boolean hasConnectionLimit()
	{
		val connectionLimit = getProperty("api.server.connectionLimit");
		return !StringUtils.isEmpty(connectionLimit);
	}

	private int getConnectionLimit()
	{
		return Integer.parseInt(getProperty("api.server.connectionLimit"));
	}

	private boolean isSoapEnabled()
	{
		return getBooleanProperty("api.soap.enabled", true);
	}

	private boolean isSslEnabled()
	{
		return getBooleanProperty("api.ssl.enabled", false);
	}

	private boolean isHealthEnabled()
	{
		return getBooleanProperty("api.health.enabled", false);
	}

	private boolean isJmxEnabled()
	{
		return getBooleanProperty("api.jmx.enabled", false);
	}

	private boolean isHeadless()
	{
		return getBooleanProperty("api.headless", false);
	}

	private ServerConnector createHttpConnector()
	{
		val httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		val result = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
		result.setHost(getProperty("api.host", DEFAULT_HOST));
		result.setPort(getIntegerProperty("api.port", 8080));
		result.setName(WEB_CONNECTOR_NAME);
		if (!isHeadless())
			println("Web Server configured on http://" + Utils.getHost(result.getHost()) + ":" + result.getPort() + getPath());
		if (isSoapEnabled())
			println("SOAP Service configured on http://" + Utils.getHost(result.getHost()) + ":" + result.getPort() + SOAP_URL);
		return result;
	}

	protected void initHealthServer()
	{
		val connector = createHealthConnector(server);
		server.addConnector(connector);
	}

	private ServerConnector createHealthConnector(Server server)
	{
		val result = new ServerConnector(server);
		result.setHost(getProperty("api.host", DEFAULT_HOST));
		result.setPort(getIntegerProperty("api.health.port", 8008));
		result.setName(HEALTH_CONNECTOR_NAME);
		println("Health Service configured on http://" + Utils.getHost(result.getHost()) + ":" + result.getPort() + HEALTH_URL);
		return result;
	}

	private SslContextFactory.Server createSslContextFactory() throws GeneralSecurityException, IOException
	{
		val keyStorePassword = getProperty("api.ssl.keyStorePassword");
		if (StringUtils.isBlank(keyStorePassword) || "password".equals(keyStorePassword))
			throw new IllegalArgumentException("A non-default keystore password must be provided using api.ssl.keyStorePassword");
		val result = new SslContextFactory.Server();
		val ebMSKeyStore = EbMSKeyStore.of(
				KeyStoreType.valueOf(getProperty("api.ssl.keyStoreType", DEFAULT_KEYSTORE_TYPE)),
				getProperty("api.ssl.keyStorePath", DEFAULT_KEYSTORE_FILE),
				keyStorePassword);
		addKeyStore(result, ebMSKeyStore);
		if (isClientAuthenticationEnabled())
			addTrustStore(result);
		return result;
	}

	private boolean isClientAuthenticationEnabled()
	{
		return getBooleanProperty("api.ssl.clientAuthentication", false);
	}

	private void addKeyStore(SslContextFactory.Server sslContextFactory, EbMSKeyStore ebMSKeyStore)
	{
		val protocols = getProperty("api.ssl.protocols");
		if (!StringUtils.isEmpty(protocols))
			sslContextFactory.setIncludeProtocols(StringUtils.stripAll(StringUtils.split(protocols, ',')));
		val cipherSuites = getProperty("api.ssl.cipherSuites");
		if (!StringUtils.isEmpty(cipherSuites))
			sslContextFactory.setIncludeCipherSuites(StringUtils.stripAll(StringUtils.split(cipherSuites, ',')));
		sslContextFactory.setKeyStore(ebMSKeyStore.getKeyStore());
		sslContextFactory.setKeyStorePassword(ebMSKeyStore.getPassword());
	}

	private void addTrustStore(SslContextFactory.Server sslContextFactory) throws IOException
	{
		val trustStoreType = getProperty("api.ssl.trustStoreType", DEFAULT_KEYSTORE_TYPE);
		val trustStorePath = getProperty("api.ssl.trustStorePath");
		val trustStorePassword = getProperty("api.ssl.trustStorePassword");
		val trustStore = getResource(trustStorePath);
		if (trustStore != null && trustStore.exists())
		{
			println("Using trustStore " + trustStore.getURI());
			sslContextFactory.setNeedClientAuth(true);
			sslContextFactory.setTrustStoreType(trustStoreType);
			sslContextFactory.setTrustStoreResource(trustStore);
			sslContextFactory.setTrustStorePassword(trustStorePassword);
		}
		else
		{
			println("Web Server not available: trustStore " + trustStorePath + " not found!");
			exit(1);
		}
	}

	private ServerConnector createHttpsConnector(SslContextFactory.Server sslContextFactory)
	{
		val httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		httpConfig.addCustomizer(new SecureRequestCustomizer(!isHostnameVerificationDisabled()));
		val result = new ServerConnector(server, sslContextFactory, new HttpConnectionFactory(httpConfig));
		result.setHost(getProperty("api.host", DEFAULT_HOST));
		result.setPort(getIntegerProperty("api.port", 8443));
		result.setName(WEB_CONNECTOR_NAME);
		if (!isHeadless())
			println("Web Server configured on https://" + Utils.getHost(result.getHost()) + ":" + result.getPort() + getPath());
		if (isSoapEnabled())
			println("SOAP Service configured on https://" + Utils.getHost(result.getHost()) + ":" + result.getPort() + SOAP_URL);
		return result;
	}

	protected String getPath()
	{
		return getProperty("api.path", DEFAULT_PATH);
	}

	private boolean isHostnameVerificationDisabled()
	{
		return getBooleanProperty("api.disableHostnameVerification", false);
	}

	protected void initJMX(Server server) throws Exception
	{
		println("Starting JMX Server...");
		val mBeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
		server.addBean(mBeanContainer);
		val jmxURL = new JMXServiceURL("rmi", null, getIntegerProperty("api.jmx.port", 1999), "/jndi/rmi:///jmxrmi");
		val sslContextFactory = isSslEnabled() ? createSslContextFactory() : null;
		val jmxServer = new ConnectorServer(jmxURL, createEnv(), "org.eclipse.jetty.jmx:name=rmiconnectorserver", sslContextFactory);
		server.addBean(jmxServer);
		println("JMX Server configured on " + jmxURL);
	}

	private Map<String, Object> createEnv()
	{
		val result = new HashMap<String, Object>();
		if (hasJmxAccessFile() && hasJmxPasswordFile())
		{
			result.put("jmx.remote.x.access.file", getProperty("api.jmx.accessFile"));
			result.put("jmx.remote.x.password.file", getProperty("api.jmx.passwordFile"));
		}
		return result;
	}

	private boolean hasJmxAccessFile()
	{
		return !StringUtils.isEmpty(getProperty("api.jmx.accessFile"));
	}

	private boolean hasJmxPasswordFile()
	{
		return !StringUtils.isEmpty(getProperty("api.jmx.passwordFile"));
	}

	protected ServletContextHandler createWebContextHandler(ContextLoaderListener contextLoaderListener) throws Exception
	{
		val result = new ServletContextHandler(ServletContextHandler.SESSIONS);
		result.setVirtualHosts(List.of("@" + WEB_CONNECTOR_NAME));
		result.setInitParameter("configuration", "deployment");
		result.setContextPath(getPath());
		if (hasEchoHeaderNames())
			result.addFilter(createEchoServletFilterHolder(getProperty("api.logging.echoHeaderNames")), "/*", EnumSet.allOf(DispatcherType.class));
		if (hasMdcHeaderNames())
			result.addFilter(createMDCServletFilterHolder(getProperty("api.logging.mdcHeaderNames")), "/*", EnumSet.allOf(DispatcherType.class));
		if (isAuditLoggingEnabled())
			result.addFilter(createRemoteAddressMDCFilterHolder(), "/*", EnumSet.allOf(DispatcherType.class));
		if (hasRateLimit())
			result.addFilter(createRateLimiterFilterHolder(getProperty("api.server.queriesPerSecond")), "/*", EnumSet.allOf(DispatcherType.class));
		if (hasUserRateLimit())
			result.addFilter(createUserRateLimiterFilterHolder(getProperty("api.server.userQueriesPerSecond")), "/*", EnumSet.allOf(DispatcherType.class));
		if (isAuthenticationEnabled())
			addAuthenticationHandler(result);
		if (isSoapEnabled())
			result.addServlet(CXFServlet.class, SOAP_URL + "/*");
		result.setErrorHandler(createErrorHandler());
		result.addEventListener(contextLoaderListener);
		return result;
	}

	private boolean hasEchoHeaderNames()
	{
		return !StringUtils.isEmpty(getProperty("api.logging.echoHeaderNames"));
	}

	private boolean hasMdcHeaderNames()
	{
		return !StringUtils.isEmpty(getProperty("api.logging.mdcHeaderNames"));
	}

	private boolean isAuditLoggingEnabled()
	{
		return getBooleanProperty("api.logging.audit.enabled", false);
	}

	private boolean hasRateLimit()
	{
		return !StringUtils.isEmpty(getProperty("api.server.queriesPerSecond"));
	}

	private boolean hasUserRateLimit()
	{
		return !StringUtils.isEmpty(getProperty("api.server.userQueriesPerSecond"));
	}

	private boolean isAuthenticationEnabled()
	{
		return getBooleanProperty("api.authentication.enabled", false);
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

	protected FilterHolder createEchoServletFilterHolder(String headerNames)
	{
		val result = new FilterHolder(nl.clockwork.ebms.server.endpoint.servlet.filters.EchoServletFilter.class);
		result.setInitParameter("headerNames", headerNames);
		return result;
	}

	protected FilterHolder createMDCServletFilterHolder(String headerNames)
	{
		val result = new FilterHolder(nl.clockwork.ebms.server.endpoint.servlet.filters.MDCServletFilter.class);
		result.setInitParameter("headerNames", headerNames);
		return result;
	}

	protected FilterHolder createRemoteAddressMDCFilterHolder()
	{
		return new FilterHolder(nl.clockwork.ebms.server.endpoint.servlet.filters.RemoteAddressMDCFilter.class);
	}

	protected FilterHolder createRateLimiterFilterHolder(String queriesPerSecond)
	{
		val result = new FilterHolder(nl.clockwork.ebms.server.endpoint.servlet.filters.RateLimiterFilter.class);
		result.setInitParameter("api.server.queriesPerSecond", queriesPerSecond);
		return result;
	}

	protected FilterHolder createUserRateLimiterFilterHolder(String queriesPerSecond)
	{
		val result = new FilterHolder(nl.clockwork.ebms.server.endpoint.servlet.filters.RateLimiterFilter.class);
		result.setInitParameter("api.server.userQueriesPerSecond", queriesPerSecond);
		return result;
	}

	private FilterHolder createClientCertificateAuthenticationFilterHolder() throws IOException
	{
		println("Configuring Web Server client certificate authentication:");
		val result = new FilterHolder(nl.clockwork.ebms.server.endpoint.servlet.filters.ClientCertificateAuthenticationFilter.class);
		val clientTrustStoreType = getProperty("api.ssl.clientTrustStoreType", DEFAULT_KEYSTORE_TYPE);
		val clientTrustStorePath = getProperty("api.ssl.clientTrustStorePath");
		val clientTrustStorePassword = getProperty("api.ssl.clientTrustStorePassword");
		val trustStore = getResource(clientTrustStorePath);
		if (trustStore != null && trustStore.exists())
		{
			println("Using clientTrustStore " + trustStore.getURI());
			result.setInitParameter("trustStoreType", clientTrustStoreType);
			result.setInitParameter("trustStorePath", clientTrustStorePath);
			result.setInitParameter("trustStorePassword", clientTrustStorePassword);
			return result;
		}
		else
		{
			println("Web Server not available: clientTrustStore " + clientTrustStorePath + " not found!");
			exit(1);
			return null;
		}
	}

	protected FilterHolder createClientCertificateManagerFilterHolder(String clientCertificateHeader)
	{
		val result = new FilterHolder(nl.clockwork.ebms.server.endpoint.servlet.filters.ClientCertificateManagerFilter.class);
		result.setInitParameter("x509CertificateHeader", clientCertificateHeader);
		return result;
	}

	private void addAuthenticationHandler(ServletContextHandler result) throws IOException, NoSuchAlgorithmException
	{
		if (!isClientAuthenticationEnabled())
		{
			println("Configuring Web Server basic authentication:");
			val file = new File(REALM_FILE);
			if (file.exists())
				println("Using file " + file.getAbsoluteFile());
			else
				createRealmFile(file);
			result.setSecurityHandler(getSecurityHandler());
		}
		else if (isSslEnabled())
		{
			result.addFilter(
					createClientCertificateManagerFilterHolder(getProperty("api.clientCertificateHeader")),
					"/*",
					EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
			result.addFilter(createClientCertificateAuthenticationFilterHolder(), "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
		}
	}

	private ErrorPageErrorHandler createErrorHandler()
	{
		val result = new ErrorPageErrorHandler();
		result.setErrorPages(Map.of("404", "/404"));
		return result;
	}

	protected ServletContextHandler createHealthContextHandler() throws Exception
	{
		val result = new ServletContextHandler();
		result.setVirtualHosts(List.of("@" + HEALTH_CONNECTOR_NAME));
		result.setInitParameter("configuration", "deployment");
		result.setContextPath(DEFAULT_PATH);
		result.addServlet(HealthServlet.class, HEALTH_URL + "/*");
		return result;
	}

	protected Resource getResource(String path) throws IOException
	{
		val resourceFactory = ResourceFactory.root();
		val result = resourceFactory.newResource(path);
		if (result.exists())
			return result;
		val url = Start.class.getClassLoader().getResource(path);
		return url == null ? result : resourceFactory.newResource(url);
	}

	protected void createRealmFile(File file) throws IOException
	{
		val builder = prompter.newBuilder();
		builder.createInputPrompt().name("username").message("enter username").defaultValue("admin").addPrompt();
		val results = prompter.prompt(Collections.emptyList(), builder.build());
		val username = ((InputResult)results.get("username")).getInput();
		val password = readPassword();
		println("Writing to file: " + file.getAbsoluteFile());
		FileUtils.writeStringToFile(file, username + ": " + password + ",user", Charset.defaultCharset(), false);
	}

	private String readPassword() throws IOException
	{
		while (true)
		{
			val builder = prompter.newBuilder();
			builder.createInputPrompt()
					.name(PASSWORD_PROMPT)
					.message("enter password")
					.mask('*')
					.validator(input -> input.length() >= MIN_PASSWORD_LENGTH)
					.filter(this::toBCrypt)
					.addPrompt();
			builder.createInputPrompt().name("password2").message("re-enter password").mask('*').filter(this::toBCrypt).addPrompt();
			val results = prompter.prompt(Collections.emptyList(), builder.build());
			val result = ((InputResult)results.get(PASSWORD_PROMPT)).getInput();
			val password = ((InputResult)results.get("password2")).getInput();
			if (result.equals(password))
				return result;
			else
				println("Passwords don't match! Try again.");
		}
	}

	private String toBCrypt(String s)
	{
		return BCrypt.hashpw(s, BCrypt.gensalt());
	}

	private static Terminal createTerminal()
	{
		try
		{
			return TerminalBuilder.builder().system(true).build();
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	protected SecurityHandler getSecurityHandler()
	{
		val result = new ConstraintSecurityHandler();
		val constraint = createSecurityConstraint();
		val mapping = createSecurityConstraintMapping(constraint);
		result.setConstraintMappings(Collections.singletonList(mapping));
		result.setAuthenticator(new BasicAuthenticator());
		result.setLoginService(new HashLoginService(REALM, createResource(REALM_FILE)));
		return result;
	}

	private Resource createResource(String realmFile)
	{
		return ResourceFactory.of(new ResourceHandler()).newResource(Path.of(realmFile));
	}

	private Constraint createSecurityConstraint()
	{
		return new Constraint.Builder().name("auth").roles("user", "admin").authorization(Authorization.FORBIDDEN).build();
	}

	private ConstraintMapping createSecurityConstraintMapping(final Constraint constraint)
	{
		val result = new ConstraintMapping();
		result.setPathSpec("/*");
		result.setConstraint(constraint);
		return result;
	}
}
