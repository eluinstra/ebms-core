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
package nl.clockwork.ebms.migrate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

/**
 * Migrates the command line parameters of EbMS Admin 2.20.x to the Spring properties used by EbMS Core and Server 2.21.x.
 * <p>
 * The mapping below reflects the connector flags of the 2.20 {@code Start} class and the property keys that the 2.21 embedded {@code Start} and the
 * {@code @Value}-annotated Spring beans of the {@code common}/{@code core} modules actually read.
 * </p>
 */
public final class MigrateAdmin20ToServer21
{
	private final CommandLine cmd;

	public MigrateAdmin20ToServer21(CommandLine cmd)
	{
		this.cmd = cmd;
	}

	public MigrateResult migrate()
	{
		val result = new LinkedHashMap<String, String>();
		val ignored = new ArrayList<String>();
		val warnings = new ArrayList<String>();

		boolean ssl = hasOption(AdminOptions.SSL_OPTION);

		// api.*: connector flags of the 2.21 embedded Start (nl.clockwork.ebms.server.embedded.startup.Start).
		// The core connector fields are always emitted (with the 2.20 defaults) so the result is a runnable config.
		result.put("api.host", get(AdminOptions.HOST_OPTION, AdminOptions.DEFAULT_HOST));
		result.put("api.port", get(AdminOptions.PORT_OPTION, ssl ? AdminOptions.DEFAULT_SSL_PORT : AdminOptions.DEFAULT_PORT));
		result.put("api.path", get(AdminOptions.PATH_OPTION, AdminOptions.DEFAULT_PATH));
		putFlag(result, "api.soap.enabled", AdminOptions.SOAP_OPTION);
		putFlag(result, "api.headless", AdminOptions.HEADLESS_OPTION);
		putFlag(result, "api.health.enabled", AdminOptions.HEALTH_OPTION);
		if (hasOption(AdminOptions.HEALTH_OPTION))
			result.put("api.health.port", get(AdminOptions.HEALTH_PORT_OPTION, AdminOptions.DEFAULT_HEALTH_PORT));
		putIfPresent(result, "api.server.connectionLimit", get(AdminOptions.CONNECTION_LIMIT_OPTION));
		putIfPresent(result, "api.server.queriesPerSecond", get(AdminOptions.QUERIES_PER_SECOND_OPTION));
		putIfPresent(result, "api.server.userQueriesPerSecond", get(AdminOptions.USER_QUERIES_PER_SECOND_OPTION));
		putFlag(result, "api.logging.audit.enabled", AdminOptions.AUDIT_LOGGING_OPTION);
		putIfPresent(result, "api.logging.echoHeaderNames", get(AdminOptions.ECHO_HEADER_NAMES_OPTION));
		putIfPresent(result, "api.logging.mdcHeaderNames", get(AdminOptions.MDC_HEADER_NAMES_OPTION));
		putFlag(result, "api.ssl.enabled", AdminOptions.SSL_OPTION);
		putFlag(result, "api.authentication.enabled", AdminOptions.AUTHENTICATION_OPTION);
		putFlag(result, "api.disableHostnameVerification", AdminOptions.DISABLE_HOSTNAME_VERIFICATION_OPTION);
		putIfPresent(result, "api.clientCertificateHeader", get(AdminOptions.CLIENT_CERTIFICATE_HEADER_OPTION));
		putIfPresent(result, "api.configDir", get(AdminOptions.CONFIG_DIR_OPTION));
		// JMX was removed in 2.21: keep the option values as api.* properties but warn
		putFlag(result, "api.jmx.enabled", AdminOptions.JMX_OPTION);
		if (hasOption(AdminOptions.JMX_OPTION))
		{
			result.put("api.jmx.port", get(AdminOptions.JMX_PORT_OPTION, AdminOptions.DEFAULT_JMX_PORT));
			warnings.add("JMX is no longer supported by the 2.21 embedded server; the api.jmx.* properties are kept for reference only.");
		}
		putIfPresent(result, "api.jmx.accessFile", get(AdminOptions.JMX_ACCESS_FILE_OPTION));
		putIfPresent(result, "api.jmx.passwordFile", get(AdminOptions.JMX_PASSWORD_FILE_OPTION));

		// api.ssl.*: emitted when SSL is on (matching the 2.20 Start, which configured these only with -ssl).
		if (ssl)
		{
			putIfPresent(result, "api.ssl.protocols", get(AdminOptions.PROTOCOLS_OPTION));
			putIfPresent(result, "api.ssl.cipherSuites", get(AdminOptions.CIPHER_SUITES_OPTION));
			result.put("api.ssl.keyStoreType", get(AdminOptions.KEY_STORE_TYPE_OPTION, AdminOptions.DEFAULT_KEYSTORE_TYPE));
			result.put("api.ssl.keyStorePath", get(AdminOptions.KEY_STORE_PATH_OPTION, AdminOptions.DEFAULT_KEYSTORE_FILE));
			putIfPresent(result, "api.ssl.keyStorePassword", get(AdminOptions.KEY_STORE_PASSWORD_OPTION));
			putFlag(result, "api.ssl.clientAuthentication", AdminOptions.CLIENT_AUTHENTICATION_OPTION);
			if (hasOption(AdminOptions.CLIENT_AUTHENTICATION_OPTION))
			{
				result.put("api.ssl.trustStoreType", get(AdminOptions.TRUST_STORE_TYPE_OPTION, AdminOptions.DEFAULT_KEYSTORE_TYPE));
				putIfPresent(result, "api.ssl.trustStorePath", get(AdminOptions.TRUST_STORE_PATH_OPTION));
				putIfPresent(result, "api.ssl.trustStorePassword", get(AdminOptions.TRUST_STORE_PASSWORD_OPTION));
			}
			else if (hasOption(AdminOptions.TRUST_STORE_PATH_OPTION)
					|| hasOption(AdminOptions.TRUST_STORE_TYPE_OPTION)
					|| hasOption(AdminOptions.TRUST_STORE_PASSWORD_OPTION))
			{
				result.put("api.ssl.trustStoreType", get(AdminOptions.TRUST_STORE_TYPE_OPTION, AdminOptions.DEFAULT_KEYSTORE_TYPE));
				putIfPresent(result, "api.ssl.trustStorePath", get(AdminOptions.TRUST_STORE_PATH_OPTION));
				putIfPresent(result, "api.ssl.trustStorePassword", get(AdminOptions.TRUST_STORE_PASSWORD_OPTION));
			}
			if (hasOption(AdminOptions.CLIENT_TRUST_STORE_TYPE_OPTION)
					|| hasOption(AdminOptions.CLIENT_TRUST_STORE_PATH_OPTION)
					|| hasOption(AdminOptions.CLIENT_TRUST_STORE_PASSWORD_OPTION))
			{
				result.put("api.ssl.clientTrustStoreType", get(AdminOptions.CLIENT_TRUST_STORE_TYPE_OPTION, AdminOptions.DEFAULT_KEYSTORE_TYPE));
				putIfPresent(result, "api.ssl.clientTrustStorePath", get(AdminOptions.CLIENT_TRUST_STORE_PATH_OPTION));
				putIfPresent(result, "api.ssl.clientTrustStorePassword", get(AdminOptions.CLIENT_TRUST_STORE_PASSWORD_OPTION));
			}
		}

		if (hasOption(AdminOptions.HELP_OPTION))
			ignored.add(AdminOptions.HELP_OPTION);

		validate(result, warnings);

		return new MigrateResult(result, ignored, warnings);
	}

	private void validate(Map<String, String> result, List<String> warnings)
	{
		if (Boolean.parseBoolean(result.get("api.ssl.enabled")))
		{
			if (StringUtils.isBlank(result.get("api.ssl.keyStorePassword")))
				warnings.add("api.ssl.enabled=true but no -keyStorePassword was given; the 2.20 server required a non-default keystore password.");
			if (Boolean.parseBoolean(result.get("api.ssl.clientAuthentication")) && StringUtils.isBlank(result.get("api.ssl.trustStorePath")))
				warnings.add("api.ssl.clientAuthentication=true but no -trustStorePath was given; the 2.20 server would have failed to start without one.");
		}
	}

	private String get(String option)
	{
		return cmd.getOptionValue(option);
	}

	private String get(String option, String defaultValue)
	{
		return cmd.getOptionValue(option, defaultValue);
	}

	private boolean hasOption(String option)
	{
		return cmd.hasOption(option);
	}

	private void putIfPresent(Map<String, String> result, String key, String value)
	{
		if (StringUtils.isNotBlank(value))
			result.put(key, value);
	}

	private void putFlag(Map<String, String> result, String key, String option)
	{
		result.put(key, Boolean.toString(cmd.hasOption(option)));
	}

	/**
	 * Parses the given arguments against the 2.20 Admin option set.
	 *
	 * @param args the command line arguments.
	 * @return the parsed command line.
	 * @throws org.apache.commons.cli.ParseException if an unknown option is given.
	 */
	public static CommandLine parseAdminCommandLine(String[] args) throws org.apache.commons.cli.ParseException
	{
		return new org.apache.commons.cli.DefaultParser().parse(AdminOptions.createOptions(), args);
	}
}
