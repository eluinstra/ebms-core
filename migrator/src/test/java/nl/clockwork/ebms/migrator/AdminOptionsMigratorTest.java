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
		// api.soap.enabled is always emitted: SOAP was off by default in 2.20.x but defaults to true in 2.21.x
		assertThat(migrate()).containsOnly(Map.entry("api.soap.enabled", "false"));
	}

	@Test
	void shouldMigrateHostToApiConnectorOnly() throws Exception
	{
		// in 2.20.x --host configured the web connector only; the ebms connector reads ebms.host from the properties file
		val properties = migrate("--host", "10.0.0.1");
		assertThat(properties).containsEntry("api.host", "10.0.0.1").doesNotContainKey("ebms.host");
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
				// the web options must not leak into the EbMS connector settings
				.doesNotContainKey("ebms.ssl")
				.doesNotContainKey("https.protocols")
				.doesNotContainKey("https.cipherSuites")
				.doesNotContainKey("https.requireClientAuthentication")
				.doesNotContainKey("keystore.type")
				.doesNotContainKey("keystore.path")
				.doesNotContainKey("keystore.password")
				.doesNotContainKey("truststore.type")
				.doesNotContainKey("truststore.path")
				.doesNotContainKey("truststore.password");
	}

	@Test
	void shouldMigrateSoapDisabledByDefault() throws Exception
	{
		// in 2.20.x the SOAP service was off unless --soap was given, but api.soap.enabled defaults to true in 2.21.x
		assertThat(migrate("--port", "9090")).containsEntry("api.soap.enabled", "false");
	}

	@Test
	void shouldMigrateSoapEnabled() throws Exception
	{
		assertThat(migrate("--soap")).containsEntry("api.soap.enabled", "true");
	}

	@Test
	void shouldMigrateDisableHostnameVerificationToApi() throws Exception
	{
		assertThat(migrate("--disableHostnameVerification")).containsEntry("api.disableHostnameVerification", "true");
	}

	@Test
	void shouldMigrateClientCertificateHeaderToApi() throws Exception
	{
		assertThat(migrate("--clientCertificateHeader", "X-Ssl-Cert")).containsEntry("api.clientCertificateHeader", "X-Ssl-Cert");
	}

	@Test
	void shouldOnlyMigrateKeystoreOptionsWithSslEnabled() throws Exception
	{
		// in 2.20.x the keystore options configured the web connector and were ignored without --ssl
		val properties = migrate("--keyStorePath", "/etc/ebms/keystore.p12", "--keyStorePassword", "secret");
		assertThat(properties).doesNotContainKey("api.ssl.enabled")
				.doesNotContainKey("api.ssl.keyStorePath")
				.doesNotContainKey("keystore.path")
				.doesNotContainKey("keystore.password");
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
		val options = AdminStartupOptions.parse("--soap", "--jmx", "--headless", "--keyStoresType", "PKCS12", "--clientCertificateHeader", "X-Ssl-Cert");
		val warnings = new AdminOptionsMigrator().warnings(options);
		assertThat(warnings).anyMatch(w -> w.startsWith("--soap"))
				.anyMatch(w -> w.startsWith("--jmx"))
				.anyMatch(w -> w.startsWith("--headless"))
				.anyMatch(w -> w.startsWith("--keyStoresType"))
				.anyMatch(w -> w.startsWith("--clientCertificateHeader"));
	}

	@Test
	void shouldNotWarnAboutMigratedHostnameVerification() throws Exception
	{
		val warnings = new AdminOptionsMigrator().warnings(AdminStartupOptions.parse("--disableHostnameVerification"));
		assertThat(warnings).isEmpty();
	}

	@Test
	void shouldWarnWhenSslWithoutKeyStorePassword() throws Exception
	{
		val warnings = new AdminOptionsMigrator().warnings(AdminStartupOptions.parse("--ssl"));
		assertThat(warnings).anyMatch(w -> w.startsWith("validation: --ssl without --keyStorePassword"));
	}

	@Test
	void shouldWarnWhenClientAuthenticationWithoutTrustStorePath() throws Exception
	{
		val warnings = new AdminOptionsMigrator().warnings(AdminStartupOptions.parse("--ssl", "--clientAuthentication"));
		assertThat(warnings).anyMatch(w -> w.startsWith("validation: --clientAuthentication without --trustStorePath"));
	}

	@Test
	void shouldNotWarnWhenSslOptionsAreComplete() throws Exception
	{
		val warnings = new AdminOptionsMigrator()
				.warnings(AdminStartupOptions.parse("--ssl", "--keyStorePassword", "secret", "--clientAuthentication", "--trustStorePath", "/trust.p12"));
		assertThat(warnings).noneMatch(w -> w.startsWith("validation:"));
	}

	@Test
	void shouldWarnWhenKeyStoreWithoutSsl() throws Exception
	{
		val warnings = new AdminOptionsMigrator().warnings(AdminStartupOptions.parse("--keyStorePath", "/etc/ebms/keystore.p12", "--keyStorePassword", "secret"));
		assertThat(warnings).anyMatch(w -> w.startsWith("validation: --keyStore* given without --ssl"));
	}

	@Test
	void shouldWarnWhenTrustStoreWithoutSsl() throws Exception
	{
		val warnings = new AdminOptionsMigrator().warnings(AdminStartupOptions.parse("--trustStorePath", "/etc/ebms/trust.p12"));
		assertThat(warnings).anyMatch(w -> w.startsWith("validation: --trustStore* given without --ssl"));
	}

	@Test
	void shouldWarnWhenClientTrustStoreWithoutClientCertificateAuthentication() throws Exception
	{
		val warnings =
				new AdminOptionsMigrator().warnings(AdminStartupOptions.parse("--ssl", "--clientAuthentication", "--clientTrustStorePath", "/etc/ebms/client.ts"));
		assertThat(warnings).anyMatch(w -> w.startsWith("validation: --clientTrustStore* given without"));
	}

	@Test
	void shouldNotWarnWhenClientTrustStoreWithClientCertificateAuthentication() throws Exception
	{
		val warnings = new AdminOptionsMigrator().warnings(
				AdminStartupOptions.parse(
						"--ssl",
						"--keyStorePassword",
						"secret",
						"--clientAuthentication",
						"--trustStorePath",
						"/trust.p12",
						"--authentication",
						"--clientTrustStorePath",
						"/client.ts"));
		assertThat(warnings).noneMatch(w -> w.startsWith("validation:"));
	}

	@Test
	void shouldRenderApiPropertiesAsVmArgs() throws Exception
	{
		val migrator = new AdminOptionsMigrator();
		val properties = migrator.migrate(AdminStartupOptions.parse("--port", "9090", "--ssl", "--disableEbMSClient"), Map.of());
		assertThat(migrator.toVmArguments(properties)).contains("-Dapi.port=9090", "-Dapi.ssl.enabled=true").allMatch(arg -> arg.startsWith("-Dapi."));
	}
}
