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

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Catalog of the command line parameters of the (legacy) EbMS Admin 2.20.x {@code Start} class. The option names, defaults and descriptions are a 1:1 copy of
 * the 2.20 parameter set so that a 2.20 startup command line can be parsed by this tool.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AdminOptions
{
	public static final String HELP_OPTION = "help";

	public static final String HOST_OPTION = "host";
	public static final String PORT_OPTION = "port";
	public static final String PATH_OPTION = "path";
	public static final String DISABLE_HOSTNAME_VERIFICATION_OPTION = "disableHostnameVerification";
	public static final String SOAP_OPTION = "soap";
	public static final String HEADLESS_OPTION = "headless";
	public static final String HEALTH_OPTION = "health";
	public static final String HEALTH_PORT_OPTION = "healthPort";
	public static final String CONNECTION_LIMIT_OPTION = "connectionLimit";
	public static final String QUERIES_PER_SECOND_OPTION = "queriesPerSecond";
	public static final String USER_QUERIES_PER_SECOND_OPTION = "userQueriesPerSecond";
	public static final String AUDIT_LOGGING_OPTION = "auditLogging";
	public static final String ECHO_HEADER_NAMES_OPTION = "echoHeaderNames";
	public static final String MDC_HEADER_NAMES_OPTION = "mdcHeaderNames";
	public static final String SSL_OPTION = "ssl";
	public static final String PROTOCOLS_OPTION = "protocols";
	public static final String CIPHER_SUITES_OPTION = "cipherSuites";
	public static final String KEY_STORE_TYPE_OPTION = "keyStoreType";
	public static final String KEY_STORE_PATH_OPTION = "keyStorePath";
	public static final String KEY_STORE_PASSWORD_OPTION = "keyStorePassword";
	public static final String CLIENT_AUTHENTICATION_OPTION = "clientAuthentication";
	public static final String CLIENT_CERTIFICATE_HEADER_OPTION = "clientCertificateHeader";
	public static final String TRUST_STORE_TYPE_OPTION = "trustStoreType";
	public static final String TRUST_STORE_PATH_OPTION = "trustStorePath";
	public static final String TRUST_STORE_PASSWORD_OPTION = "trustStorePassword";
	public static final String AUTHENTICATION_OPTION = "authentication";
	public static final String CLIENT_TRUST_STORE_TYPE_OPTION = "clientTrustStoreType";
	public static final String CLIENT_TRUST_STORE_PATH_OPTION = "clientTrustStorePath";
	public static final String CLIENT_TRUST_STORE_PASSWORD_OPTION = "clientTrustStorePassword";
	public static final String CONFIG_DIR_OPTION = "configDir";
	public static final String JMX_OPTION = "jmx";
	public static final String JMX_PORT_OPTION = "jmxPort";
	public static final String JMX_ACCESS_FILE_OPTION = "jmxAccessFile";
	public static final String JMX_PASSWORD_FILE_OPTION = "jmxPasswordFile";

	public static final String DEFAULT_HOST = "0.0.0.0";
	public static final String DEFAULT_PORT = "8080";
	public static final String DEFAULT_SSL_PORT = "8443";
	public static final String DEFAULT_PATH = "/";
	public static final String DEFAULT_HEALTH_PORT = "8008";
	public static final String DEFAULT_JMX_PORT = "1999";
	public static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";
	public static final String DEFAULT_KEYSTORE_FILE = "nl/clockwork/ebms/keystore.p12";
	public static final String DEFAULT_CONFIG_DIR = "";

	/**
	 * All option names of the 2.20 Admin parameter set, in the order the 2.20 {@code Start} class defined them.
	 */
	public static final List<String> OPTION_NAMES = List.of(
			HELP_OPTION,
			HOST_OPTION,
			PORT_OPTION,
			PATH_OPTION,
			SOAP_OPTION,
			HEADLESS_OPTION,
			HEALTH_OPTION,
			HEALTH_PORT_OPTION,
			CONNECTION_LIMIT_OPTION,
			QUERIES_PER_SECOND_OPTION,
			USER_QUERIES_PER_SECOND_OPTION,
			AUDIT_LOGGING_OPTION,
			ECHO_HEADER_NAMES_OPTION,
			MDC_HEADER_NAMES_OPTION,
			SSL_OPTION,
			PROTOCOLS_OPTION,
			CIPHER_SUITES_OPTION,
			KEY_STORE_TYPE_OPTION,
			KEY_STORE_PATH_OPTION,
			KEY_STORE_PASSWORD_OPTION,
			CLIENT_AUTHENTICATION_OPTION,
			CLIENT_CERTIFICATE_HEADER_OPTION,
			TRUST_STORE_TYPE_OPTION,
			TRUST_STORE_PATH_OPTION,
			TRUST_STORE_PASSWORD_OPTION,
			AUTHENTICATION_OPTION,
			CLIENT_TRUST_STORE_TYPE_OPTION,
			CLIENT_TRUST_STORE_PATH_OPTION,
			CLIENT_TRUST_STORE_PASSWORD_OPTION,
			CONFIG_DIR_OPTION,
			JMX_OPTION,
			JMX_PORT_OPTION,
			JMX_ACCESS_FILE_OPTION,
			JMX_PASSWORD_FILE_OPTION);

	/**
	 * Builds the commons-cli options for the legacy 2.20 Admin command line.
	 *
	 * @return the option set, in the same order as the 2.20 {@code Start} class defined them.
	 */
	public static Options createOptions()
	{
		val result = new Options();
		result.addOption(createFlagOption(HELP_OPTION, "print this message"));
		result.addOption(createValueOption(HOST_OPTION, "set host [default: " + DEFAULT_HOST + "]"));
		result.addOption(createValueOption(PORT_OPTION, "set port [default: " + DEFAULT_PORT + " | " + DEFAULT_SSL_PORT + " with -ssl]"));
		result.addOption(createValueOption(PATH_OPTION, "set path [default: " + DEFAULT_PATH + "]"));
		result.addOption(createFlagOption(SOAP_OPTION, "start SOAP service"));
		result.addOption(createFlagOption(HEADLESS_OPTION, "start without web interface"));
		result.addOption(createFlagOption(HEALTH_OPTION, "start health service"));
		result.addOption(createValueOption(HEALTH_PORT_OPTION, "set health service port [default: " + DEFAULT_HEALTH_PORT + "]"));
		result.addOption(createValueOption(CONNECTION_LIMIT_OPTION, "set connection limit"));
		result.addOption(createValueOption(QUERIES_PER_SECOND_OPTION, "set max requests per second"));
		result.addOption(createValueOption(USER_QUERIES_PER_SECOND_OPTION, "set max requests per user per second"));
		result.addOption(createFlagOption(AUDIT_LOGGING_OPTION, "enable audit logging"));
		result.addOption(createValueOption(ECHO_HEADER_NAMES_OPTION, "set echo header names"));
		result.addOption(createValueOption(MDC_HEADER_NAMES_OPTION, "set logging MDC header names"));
		result.addOption(createFlagOption(SSL_OPTION, "enable SSL"));
		result.addOption(createValueOption(PROTOCOLS_OPTION, "set SSL Protocols"));
		result.addOption(createValueOption(CIPHER_SUITES_OPTION, "set SSL CipherSuites"));
		result.addOption(createValueOption(KEY_STORE_TYPE_OPTION, "set keystore type [default: " + DEFAULT_KEYSTORE_TYPE + "]"));
		result.addOption(createValueOption(KEY_STORE_PATH_OPTION, "set keystore path [default: " + DEFAULT_KEYSTORE_FILE + "]"));
		result.addOption(createValueOption(KEY_STORE_PASSWORD_OPTION, "set keystore password [required with -ssl]"));
		result.addOption(createFlagOption(CLIENT_AUTHENTICATION_OPTION, "enable SSL client authentication"));
		result.addOption(createValueOption(CLIENT_CERTIFICATE_HEADER_OPTION, "set client certificate header"));
		result.addOption(createValueOption(TRUST_STORE_TYPE_OPTION, "set truststore type [default: " + DEFAULT_KEYSTORE_TYPE + "]"));
		result.addOption(createValueOption(TRUST_STORE_PATH_OPTION, "set truststore path"));
		result.addOption(createValueOption(TRUST_STORE_PASSWORD_OPTION, "set truststore password"));
		result.addOption(createFlagOption(AUTHENTICATION_OPTION, "enable basic | client certificate authentication"));
		result.addOption(createValueOption(CLIENT_TRUST_STORE_TYPE_OPTION, "set client truststore type [default: " + DEFAULT_KEYSTORE_TYPE + "]"));
		result.addOption(createValueOption(CLIENT_TRUST_STORE_PATH_OPTION, "set client truststore path"));
		result.addOption(createValueOption(CLIENT_TRUST_STORE_PASSWORD_OPTION, "set client truststore password"));
		result.addOption(createValueOption(CONFIG_DIR_OPTION, "set config directory [default: startup directory]"));
		result.addOption(createFlagOption(JMX_OPTION, "start JMX server"));
		result.addOption(createValueOption(JMX_PORT_OPTION, "set JMX port [default: " + DEFAULT_JMX_PORT + "]"));
		result.addOption(createValueOption(JMX_ACCESS_FILE_OPTION, "set JMX access file"));
		result.addOption(createValueOption(JMX_PASSWORD_FILE_OPTION, "set JMX password file"));
		return result;
	}

	private static Option createFlagOption(String name, String description)
	{
		return Option.builder().argName("").longOpt(name).desc(description).build();
	}

	private static Option createValueOption(String name, String description)
	{
		return Option.builder().argName(name).longOpt(name).hasArg().desc(description).build();
	}

	/**
	 * Returns all option names of the 2.20 Admin parameter set, in definition order.
	 *
	 * @return the option names.
	 */
	public static List<String> allOptionNames()
	{
		return OPTION_NAMES;
	}
}
