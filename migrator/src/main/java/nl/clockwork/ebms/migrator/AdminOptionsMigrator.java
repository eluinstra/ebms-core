/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms.migrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.val;

/**
 * Maps the EbMS Admin 2.20.x commandline startup options onto the spring properties used by EbMS Core and Server 2.21.x.
 * <p>
 * In 2.20.x the {@code Start}/{@code StartEmbedded} options of ebms-admin configured the web connector only; the EbMS connector was configured through the
 * {@code ebms.*}, {@code https.*} and keystore properties of the 2.20 ebms-server.properties. In 2.21.x the web/API settings live under {@code api.*} (see
 * {@code nl/clockwork/ebms/server/default.properties}), so the web options are migrated there. The EbMS connector settings of the existing
 * ebms-server.properties are kept unchanged: only the genuine EbMS connector options of 2.20.x ({@code --disableEbMSServer} and {@code --disableEbMSClient})
 * are mapped.
 * </p>
 * <p>
 * Only options that were actually given are written, so that the defaults of an existing properties file are preserved. One exception: {@code --soap} is always
 * emitted, because the default differs between 2.20.x (SOAP off unless {@code --soap}) and 2.21.x ({@code api.soap.enabled} defaults to {@code true}). Options
 * without an equivalent spring property in 2.21.x are reported as warnings instead of being written.
 * </p>
 */
public class AdminOptionsMigrator
{
	private static final String TRUE = "true";
	private static final String FALSE = "false";

	/**
	 * Merges the given admin options into the given current properties.
	 *
	 * @param options the EbMS Admin 2.20.x startup options
	 * @param currentProperties the properties currently in effect (may be empty)
	 * @return the migrated properties, ordered and without null values
	 */
	public TreeMap<String, String> migrate(AdminStartupOptions options, Map<String, String> currentProperties)
	{
		val result = new TreeMap<String, String>(currentProperties);
		val apiSection = new TreeMap<String, String>();
		val ebmsSection = new TreeMap<String, String>();

		migrateWeb(options, apiSection);
		migrateEbmsConnector(options, ebmsSection);

		result.putAll(apiSection);
		result.putAll(ebmsSection);
		result.values().removeIf(v -> v == null);
		return result;
	}

	private void migrateWeb(AdminStartupOptions options, Map<String, String> api)
	{
		// in 2.20.x --host configured the web connector only; the ebms connector reads ebms.host from the properties file
		if (options.hasOption("host"))
			api.put("api.host", options.getOptionValue("host"));
		if (options.hasOption("path"))
			api.put("api.path", options.getOptionValue("path"));
		if (options.hasOption("headless"))
			api.put("api.headless", TRUE);
		// --soap must always be emitted: SOAP was off unless given in 2.20.x, but api.soap.enabled defaults to true in 2.21.x
		api.put("api.soap.enabled", options.hasOption("soap") ? TRUE : FALSE);
		if (options.hasOption("disableHostnameVerification"))
			api.put("api.disableHostnameVerification", TRUE);
		if (options.hasOption("health"))
			api.put("api.health.enabled", TRUE);
		if (options.hasOption("healthPort"))
			api.put("api.health.port", options.getOptionValue("healthPort"));
		if (options.hasOption("connectionLimit"))
			api.put("api.server.connectionLimit", options.getOptionValue("connectionLimit"));
		if (options.hasOption("queriesPerSecond"))
			api.put("api.server.queriesPerSecond", options.getOptionValue("queriesPerSecond"));
		if (options.hasOption("userQueriesPerSecond"))
			api.put("api.server.userQueriesPerSecond", options.getOptionValue("userQueriesPerSecond"));
		if (options.hasOption("auditLogging"))
			api.put("api.logging.audit.enabled", TRUE);
		if (options.hasOption("echoHeaderNames"))
			api.put("api.logging.echoHeaderNames", options.getOptionValue("echoHeaderNames"));
		if (options.hasOption("mdcHeaderNames"))
			api.put("api.logging.mdcHeaderNames", options.getOptionValue("mdcHeaderNames"));
		if (options.hasOption("clientCertificateHeader"))
			api.put("api.clientCertificateHeader", options.getOptionValue("clientCertificateHeader"));
		if (options.hasOption("configDir"))
		{
			// api.configDir drives the ebms.configDir system property used to locate the properties files,
			// and must keep the trailing File.separator convention of 2.20.x
			api.put("api.configDir", withTrailingSeparator(options.getOptionValue("configDir")));
		}
		if (options.hasOption("jmx"))
			api.put("api.jmx.enabled", TRUE);
		if (options.hasOption("jmxPort"))
			api.put("api.jmx.port", options.getOptionValue("jmxPort"));
		if (options.hasOption("jmxAccessFile"))
			api.put("api.jmx.accessFile", options.getOptionValue("jmxAccessFile"));
		if (options.hasOption("jmxPasswordFile"))
			api.put("api.jmx.passwordFile", options.getOptionValue("jmxPasswordFile"));
		if (options.hasOption("authentication"))
			api.put("api.authentication.enabled", TRUE);

		migrateWebSsl(options, api);
	}

	private void migrateWebSsl(AdminStartupOptions options, Map<String, String> api)
	{
		val ssl = options.hasOption("ssl");
		if (ssl)
			api.put("api.ssl.enabled", TRUE);
		if (options.hasOption("port"))
			// in 2.20.x --port meant the SSL port when --ssl was given, the plain port otherwise; in 2.21.x
			// both connectors use api.port and only the default differs per connector
			api.put("api.port", options.getOptionValue("port"));
		if (ssl && options.hasOption("keyStoreType"))
			api.put("api.ssl.keyStoreType", options.getOptionValue("keyStoreType"));
		if (ssl && options.hasOption("keyStorePath"))
			api.put("api.ssl.keyStorePath", options.getOptionValue("keyStorePath"));
		if (ssl && options.hasOption("keyStorePassword"))
			api.put("api.ssl.keyStorePassword", options.getOptionValue("keyStorePassword"));
		if (ssl && options.hasOption("protocols"))
			api.put("api.ssl.protocols", options.getOptionValue("protocols"));
		if (ssl && options.hasOption("cipherSuites"))
			api.put("api.ssl.cipherSuites", options.getOptionValue("cipherSuites"));
		if (ssl && options.hasOption("clientAuthentication"))
			api.put("api.ssl.clientAuthentication", TRUE);
		if (ssl && options.hasOption("trustStoreType"))
			api.put("api.ssl.trustStoreType", options.getOptionValue("trustStoreType"));
		if (ssl && options.hasOption("trustStorePath"))
			api.put("api.ssl.trustStorePath", options.getOptionValue("trustStorePath"));
		if (ssl && options.hasOption("trustStorePassword"))
			api.put("api.ssl.trustStorePassword", options.getOptionValue("trustStorePassword"));
		if (options.hasOption("clientTrustStoreType"))
			api.put("api.ssl.clientTrustStoreType", options.getOptionValue("clientTrustStoreType"));
		if (options.hasOption("clientTrustStorePath"))
			api.put("api.ssl.clientTrustStorePath", options.getOptionValue("clientTrustStorePath"));
		if (options.hasOption("clientTrustStorePassword"))
			api.put("api.ssl.clientTrustStorePassword", options.getOptionValue("clientTrustStorePassword"));
	}

	private void migrateEbmsConnector(AdminStartupOptions options, Map<String, String> ebms)
	{
		// In 2.20.x the EbMS connector was configured through the ebms-server.properties (ebms.*, https.*, keystore.*),
		// not through the command line. Only the two genuine EbMS connector options are mapped; the web options
		// (--ssl, --keyStore*, --trustStore*, --protocols, ...) must not leak into the EbMS connector settings.
		if (options.hasOption("disableEbMSServer"))
			ebms.put("ebms.server.disabled", TRUE);
		if (options.hasOption("disableEbMSClient"))
			ebms.put("ebms.client.disabled", TRUE);
	}

	public List<String> warnings(AdminStartupOptions options)
	{
		val result = new ArrayList<String>(options.warningLines());
		if (options.hasOption("soap"))
			result.add("--soap: no option needed in 2.21.x, the SOAP service is enabled by api.soap.enabled=true, which was migrated");
		if (options.hasOption("jmx"))
			result.add(
					"--jmx: the JMX server starts with the migrated api.jmx.* properties; there is no --jmx option in 2.21.x, disable it with api.jmx.enabled=false");
		if (options.hasOption("headless"))
			result.add("--headless: the 2.21.x server has no web console; api.headless=true was migrated");
		options.movedClientCertificateHeaderValue()
				.ifPresent(
						value -> result.add(
								"--clientCertificateHeader="
										+ value
										+ ": migrated to api.clientCertificateHeader; the ebms connector always reads https.clientCertificateHeader, set that manually if needed"));
		if (options.hasOption("keyStoresType"))
			result.add("--keyStoresType: was already unused in 2.20.x, the keystore type is set with --keyStoreType");
		validate(result, options);
		return result;
	}

	private void validate(List<String> warnings, AdminStartupOptions options)
	{
		if (options.hasOption("ssl"))
		{
			if (isBlank(options.getOptionValue("keyStorePassword")))
				warnings.add(
						"validation: --ssl without --keyStorePassword; the 2.20.x server (and 2.21.x api.ssl.keyStorePassword) requires a non-default keystore password");
			if (options.hasOption("clientAuthentication") && isBlank(options.getOptionValue("trustStorePath")))
				warnings.add("validation: --clientAuthentication without --trustStorePath; the 2.20.x server would have failed to start without one");
		}
		else
		{
			if (!isBlank(options.getOptionValue("keyStorePassword")) || !isBlank(options.getOptionValue("keyStorePath")))
				warnings.add("validation: --keyStore* given without --ssl; the 2.20.x server started the web connector without SSL and ignored the keystore options");
			if (!isBlank(options.getOptionValue("trustStorePath")))
				warnings.add("validation: --trustStore* given without --ssl; the 2.20.x server ignored the truststore options");
		}
		// in 2.20.x the client truststore was used only by the client-certificate authentication filter,
		// which was added only with --authentication, --clientAuthentication and --ssl
		if (options.hasOption("clientTrustStorePath")
				&& !(options.hasOption("ssl") && options.hasOption("clientAuthentication") && options.hasOption("authentication")))
			warnings.add(
					"validation: --clientTrustStore* given without --ssl, --clientAuthentication and --authentication; the 2.20.x server ignored the client truststore options");
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.isBlank();
	}

	/**
	 * The {@code api.*} properties are read by the embedded startup through {@code System.getProperty}, so they must be passed as vmArgs; this renders them as
	 * {@code -Dkey=value} arguments.
	 */
	public List<String> toVmArguments(Map<String, String> migratedProperties)
	{
		val result = new ArrayList<String>();
		for (val entry : migratedProperties.entrySet())
		{
			if (entry.getKey().startsWith("api."))
				result.add("-D" + entry.getKey() + "=" + entry.getValue());
		}
		return result;
	}

	private static String withTrailingSeparator(String configDir)
	{
		if (configDir == null || configDir.isEmpty())
			return configDir;
		val separator = System.getProperty("file.separator");
		return configDir.endsWith(separator) || configDir.endsWith("/") || configDir.endsWith("\\") ? configDir : configDir + separator;
	}
}
