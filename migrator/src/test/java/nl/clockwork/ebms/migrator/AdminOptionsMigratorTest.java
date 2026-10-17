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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Test;

class AdminOptionsMigratorTest
{
	private Map<String, String> migrate(String...args) throws Exception
	{
		return new AdminOptionsMigrator().migrate(AdminStartupOptions.parse(args), Map.of());
	}

	@Test
	void shouldMigrateNothingWithoutOptions() throws Exception
	{
		assertThat(migrate()).isEmpty();
	}

	@Test
	void shouldMigrateHostToBothConnectors() throws Exception
	{
		val properties = migrate("--host", "10.0.0.1");
		assertThat(properties).containsEntry("api.host", "10.0.0.1").containsEntry("ebms.host", "10.0.0.1");
	}

	@Test
	void shouldMigrateWebOptions() throws Exception
	{
		val properties = migrate(
				"--port",
				"9090",
				"--path",
				"/admin",
				"--health",
				"--healthPort",
				"9008",
				"--connectionLimit",
				"100",
				"--queriesPerSecond",
				"10",
				"--userQueriesPerSecond",
				"5",
				"--auditLogging",
				"--echoHeaderNames",
				"X-CorrelationId",
				"--mdcHeaderNames",
				"X-Request-Id");
		assertThat(properties).containsEntry("api.port", "9090")
				.containsEntry("api.path", "/admin")
				.containsEntry("api.health.enabled", "true")
				.containsEntry("api.health.port", "9008")
				.containsEntry("api.server.connectionLimit", "100")
				.containsEntry("api.server.queriesPerSecond", "10")
				.containsEntry("api.server.userQueriesPerSecond", "5")
				.containsEntry("api.logging.audit.enabled", "true")
				.containsEntry("api.logging.echoHeaderNames", "X-CorrelationId")
				.containsEntry("api.logging.mdcHeaderNames", "X-Request-Id");
	}

	@Test
	void shouldMigrateSslOptions() throws Exception
	{
		val properties = migrate(
				"--ssl",
				"--keyStoreType",
				"JKS",
				"--keyStorePath",
				"/etc/ebms/keystore.jks",
				"--keyStorePassword",
				"secret",
				"--protocols",
				"TLSv1.3",
				"--cipherSuites",
				"TLS_AES_256_GCM_SHA384",
				"--clientAuthentication",
				"--trustStoreType",
				"JKS",
				"--trustStorePath",
				"/etc/ebms/truststore.jks",
				"--trustStorePassword",
				"topsecret");
		assertThat(properties).containsEntry("api.ssl.enabled", "true")
				.containsEntry("api.ssl.keyStoreType", "JKS")
				.containsEntry("api.ssl.keyStorePath", "/etc/ebms/keystore.jks")
				.containsEntry("api.ssl.keyStorePassword", "secret")
				.containsEntry("api.ssl.protocols", "TLSv1.3")
				.containsEntry("api.ssl.cipherSuites", "TLS_AES_256_GCM_SHA384")
				.containsEntry("api.ssl.clientAuthentication", "true")
				.containsEntry("api.ssl.trustStoreType", "JKS")
				.containsEntry("api.ssl.trustStorePath", "/etc/ebms/truststore.jks")
				.containsEntry("api.ssl.trustStorePassword", "topsecret")
				.containsEntry("ebms.ssl", "true")
				.containsEntry("https.protocols", "TLSv1.3")
				.containsEntry("https.cipherSuites", "TLS_AES_256_GCM_SHA384")
				.containsEntry("https.requireClientAuthentication", "true")
				.containsEntry("keystore.type", "JKS")
				.containsEntry("keystore.path", "/etc/ebms/keystore.jks")
				.containsEntry("keystore.password", "secret")
				.containsEntry("truststore.type", "JKS")
				.containsEntry("truststore.path", "/etc/ebms/truststore.jks")
				.containsEntry("truststore.password", "topsecret");
	}

	@Test
	void shouldOnlyMigrateKeystoreOptionsToApiWhenSslEnabled() throws Exception
	{
		val properties = migrate("--keyStorePath", "/etc/ebms/keystore.p12", "--keyStorePassword", "secret");
		assertThat(properties).doesNotContainKey("api.ssl.enabled")
				.doesNotContainKey("api.ssl.keyStorePath")
				.containsEntry("keystore.path", "/etc/ebms/keystore.p12")
				.containsEntry("keystore.password", "secret");
	}

	@Test
	void shouldMigrateAuthenticationOptions() throws Exception
	{
		val properties =
				migrate("--authentication", "--clientTrustStorePath", "/etc/ebms/client.ts", "--clientTrustStorePassword", "pw", "--clientTrustStoreType", "JKS");
		assertThat(properties).containsEntry("api.authentication.enabled", "true")
				.containsEntry("api.ssl.clientTrustStorePath", "/etc/ebms/client.ts")
				.containsEntry("api.ssl.clientTrustStorePassword", "pw")
				.containsEntry("api.ssl.clientTrustStoreType", "JKS");
	}

	@Test
	void shouldMigrateConfigDirWithTrailingSeparator() throws Exception
	{
		val properties = migrate("--configDir", "/etc/ebms");
		val configDir = properties.get("api.configDir");
		assertThat(configDir).startsWith("/etc/ebms");
		assertThat(configDir).endsWith(System.getProperty("file.separator"));
	}

	@Test
	void shouldMigrateEmbeddedOptions() throws Exception
	{
		val properties = migrate("--disableEbMSServer", "--disableEbMSClient");
		assertThat(properties).containsEntry("ebms.server.disabled", "true").containsEntry("ebms.client.disabled", "true");
	}

	@Test
	void shouldPreserveCurrentProperties() throws Exception
	{
		val current = Map.of("ebms.jdbc.url", "jdbc:postgresql://db/ebms", "api.host", "127.0.0.1");
		val properties = new AdminOptionsMigrator().migrate(AdminStartupOptions.parse("--port", "9999"), current);
		assertThat(properties).containsEntry("ebms.jdbc.url", "jdbc:postgresql://db/ebms").containsEntry("api.host", "127.0.0.1").containsEntry("api.port", "9999");
	}

	@Test
	void shouldMigrateHeadlessToApiHeadless() throws Exception
	{
		assertThat(migrate("--headless")).containsEntry("api.headless", "true");
	}

	@Test
	void shouldWarnAboutOptionsWithoutEquivalent() throws Exception
	{
		val options =
				AdminStartupOptions.parse("--soap", "--jmx", "--keyStoresType", "PKCS12", "--clientCertificateHeader", "X-Ssl-Cert", "--disableHostnameVerification");
		val warnings = new AdminOptionsMigrator().warnings(options);
		assertThat(warnings).anyMatch(w -> w.startsWith("--soap"))
				.anyMatch(w -> w.startsWith("--jmx"))
				.anyMatch(w -> w.startsWith("--keyStoresType"))
				.anyMatch(w -> w.startsWith("--clientCertificateHeader"))
				.anyMatch(w -> w.startsWith("--disableHostnameVerification"));
	}

	@Test
	void shouldRenderApiPropertiesAsVmArgs() throws Exception
	{
		val migrator = new AdminOptionsMigrator();
		val properties = migrator.migrate(AdminStartupOptions.parse("--port", "9090", "--ssl", "--disableEbMSClient"), Map.of());
		assertThat(migrator.toVmArguments(properties)).contains("-Dapi.port=9090", "-Dapi.ssl.enabled=true").allMatch(arg -> arg.startsWith("-Dapi."));
	}
}
