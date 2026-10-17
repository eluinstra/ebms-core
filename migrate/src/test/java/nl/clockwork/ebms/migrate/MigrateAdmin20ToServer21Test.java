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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import lombok.val;
import org.junit.jupiter.api.Test;

class MigrateAdmin20ToServer21Test
{
	@Test
	void mapsConnectorFlagsToApiProperties() throws org.apache.commons.cli.ParseException
	{
		val cmd = MigrateAdmin20ToServer21
				.parseAdminCommandLine(new String[]{"-port", "8888", "-path", "/ebms", "-soap", "-health", "-healthPort", "9009", "-ssl", "-headless"});
		val result = new MigrateAdmin20ToServer21(cmd).migrate();

		assertThat(result.getProperties()).containsEntry("api.port", "8888")
				.containsEntry("api.path", "/ebms")
				.containsEntry("api.soap.enabled", "true")
				.containsEntry("api.health.enabled", "true")
				.containsEntry("api.health.port", "9009")
				.containsEntry("api.ssl.enabled", "true")
				.containsEntry("api.headless", "true")
				.containsEntry("api.ssl.keyStorePath", AdminOptions.DEFAULT_KEYSTORE_FILE)
				.containsEntry("api.ssl.keyStoreType", "PKCS12")
				.doesNotContainKey("api.ssl.keyStorePassword");
	}

	@Test
	void sslKeystoreAndTruststoreMapping() throws org.apache.commons.cli.ParseException
	{
		val cmd = MigrateAdmin20ToServer21.parseAdminCommandLine(
				new String[]{"-ssl", "-keyStoreType", "PKCS12", "-keyStorePath", "/keys/keystore.p12", "-keyStorePassword", "secret", "-protocols", "TLSv1.2,TLSv1.3",
						"-cipherSuites", "TLS_AES_128_GCM_SHA256", "-clientAuthentication", "-trustStoreType", "PKCS12", "-trustStorePath", "/keys/truststore.p12",
						"-trustStorePassword", "trustpw", "-clientTrustStorePath", "/keys/client.p12", "-clientTrustStorePassword", "clientpw"});
		val result = new MigrateAdmin20ToServer21(cmd).migrate();

		assertThat(result.getProperties()).containsEntry("api.ssl.enabled", "true")
				.containsEntry("api.ssl.keyStoreType", "PKCS12")
				.containsEntry("api.ssl.keyStorePath", "/keys/keystore.p12")
				.containsEntry("api.ssl.keyStorePassword", "secret")
				.containsEntry("api.ssl.protocols", "TLSv1.2,TLSv1.3")
				.containsEntry("api.ssl.cipherSuites", "TLS_AES_128_GCM_SHA256")
				.containsEntry("api.ssl.clientAuthentication", "true")
				.containsEntry("api.ssl.trustStoreType", "PKCS12")
				.containsEntry("api.ssl.trustStorePath", "/keys/truststore.p12")
				.containsEntry("api.ssl.trustStorePassword", "trustpw")
				.containsEntry("api.ssl.clientTrustStorePath", "/keys/client.p12")
				.containsEntry("api.ssl.clientTrustStorePassword", "clientpw");
		assertThat(result.getWarnings()).isEmpty();
	}

	@Test
	void rateLimitLoggingAndJmxMapping() throws org.apache.commons.cli.ParseException
	{
		val cmd = MigrateAdmin20ToServer21.parseAdminCommandLine(
				new String[]{"-connectionLimit", "100", "-queriesPerSecond", "50", "-userQueriesPerSecond", "10", "-auditLogging", "-echoHeaderNames", "X-Request-Id",
						"-mdcHeaderNames", "X-Trace", "-jmx", "-jmxPort", "2999", "-configDir", "/etc/ebms"});
		val result = new MigrateAdmin20ToServer21(cmd).migrate();

		assertThat(result.getProperties()).containsEntry("api.server.connectionLimit", "100")
				.containsEntry("api.server.queriesPerSecond", "50")
				.containsEntry("api.server.userQueriesPerSecond", "10")
				.containsEntry("api.logging.audit.enabled", "true")
				.containsEntry("api.logging.echoHeaderNames", "X-Request-Id")
				.containsEntry("api.logging.mdcHeaderNames", "X-Trace")
				.containsEntry("api.jmx.enabled", "true")
				.containsEntry("api.jmx.port", "2999")
				.containsEntry("api.configDir", "/etc/ebms");
		assertThat(result.getWarnings()).anyMatch(w -> w.contains("JMX"));
	}

	@Test
	void sslWithoutKeystorePasswordWarns() throws org.apache.commons.cli.ParseException
	{
		val cmd = MigrateAdmin20ToServer21.parseAdminCommandLine(new String[]{"-ssl"});
		val result = new MigrateAdmin20ToServer21(cmd).migrate();

		assertThat(result.getWarnings()).anyMatch(w -> w.contains("keyStorePassword"));
	}

	@Test
	void sslClientAuthWithoutTruststoreWarns() throws org.apache.commons.cli.ParseException
	{
		val cmd = MigrateAdmin20ToServer21.parseAdminCommandLine(new String[]{"-ssl", "-keyStorePassword", "secret", "-clientAuthentication"});
		val result = new MigrateAdmin20ToServer21(cmd).migrate();

		assertThat(result.getWarnings()).anyMatch(w -> w.contains("trustStorePath"));
	}

	@Test
	void flagsWithoutValueDefaultToFalse() throws org.apache.commons.cli.ParseException
	{
		val cmd = MigrateAdmin20ToServer21.parseAdminCommandLine(new String[]{"-port", "8888"});
		val result = new MigrateAdmin20ToServer21(cmd).migrate();

		assertThat(result.getProperties()).containsEntry("api.soap.enabled", "false")
				.containsEntry("api.headless", "false")
				.containsEntry("api.ssl.enabled", "false")
				.containsEntry("api.health.enabled", "false")
				.containsEntry("api.authentication.enabled", "false");
	}

	@Test
	void defaultsAreFilledForHostPortAndPath() throws org.apache.commons.cli.ParseException
	{
		val cmd = MigrateAdmin20ToServer21.parseAdminCommandLine(new String[]{"-soap"});
		val result = new MigrateAdmin20ToServer21(cmd).migrate();

		assertThat(result.getProperties()).containsEntry("api.host", AdminOptions.DEFAULT_HOST)
				.containsEntry("api.port", AdminOptions.DEFAULT_PORT)
				.containsEntry("api.path", AdminOptions.DEFAULT_PATH);
	}

	@Test
	void unknownAdminOptionIsRejected() throws org.apache.commons.cli.ParseException
	{
		org.junit.jupiter.api.Assertions
				.assertThrows(org.apache.commons.cli.ParseException.class, () -> MigrateAdmin20ToServer21.parseAdminCommandLine(new String[]{"-notARealOption", "1"}));
	}

	@Test
	void writerProducesReadableProperties() throws IOException, org.apache.commons.cli.ParseException
	{
		val cmd = MigrateAdmin20ToServer21.parseAdminCommandLine(new String[]{"-port", "8888", "-ssl", "-keyStorePassword", "secret"});
		val result = new MigrateAdmin20ToServer21(cmd).migrate();
		val writer = new StringWriter();
		new ServerPropertiesWriter(result.getProperties()).write(writer);

		val props = new Properties();
		props.load(new StringReader(writer.toString()));
		assertThat(props.getProperty("api.port")).isEqualTo("8888");
		assertThat(props.getProperty("api.ssl.enabled")).isEqualTo("true");
		assertThat(props.getProperty("api.ssl.keyStorePassword")).isEqualTo("secret");
	}
}
