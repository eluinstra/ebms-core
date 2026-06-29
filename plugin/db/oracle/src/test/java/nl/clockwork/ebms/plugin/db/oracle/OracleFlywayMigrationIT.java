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
package nl.clockwork.ebms.plugin.db.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Validates the Flyway migrations under {@code src/main/resources/db/migration/<variant>} by
 * applying them to a real Oracle Database 23ai Free container. Uses the official Oracle image
 * published at {@code container-registry.oracle.com/database/free} (multi-arch, built from
 * <a href="https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/README.md">
 * oracle/docker-images SingleInstance</a>) instead of the {@code gvenzl/oracle-free} image that
 * the {@code testcontainers-oracle-free} module uses by default. The image is substituted via
 * {@link DockerImageName#asCompatibleSubstituteFor(String)} so the typed {@link OracleContainer}
 * still applies. Replaces the former {@code validate-migrations.sh} harness for this plugin.
 *
 * <p>Notes:
 * <ul>
 *   <li>The official image is a prebuilt database, so {@code /opt/oracle/scripts/setup/} is not
 *       executed at start-up. Instead, we connect once as {@code system} after the container is
 *       ready and create a dedicated {@code ebms} user that Flyway then authenticates as.</li>
 *   <li>The image expects {@code ORACLE_PWD} (not {@code ORACLE_PASSWORD}) to set the
 *       sys/system/pdbadmin password.</li>
 *   <li>ORACLE_SID is fixed to {@code FREE} and the PDB is fixed to {@code FREEPDB1}.</li>
 * </ul>
 */
@Testcontainers
class OracleFlywayMigrationIT
{
	private static final String SYS_PASSWORD = "oracle";
	private static final String USER = "ebms";
	private static final String PASSWORD = "ebms";

	@Container
	static final OracleContainer ORACLE = new OracleContainer(
			DockerImageName.parse("container-registry.oracle.com/database/free:latest").asCompatibleSubstituteFor("gvenzl/oracle-free"))
			.withEnv("ORACLE_PWD", SYS_PASSWORD)
			.withUsername(USER)
			.withPassword(PASSWORD);

	@BeforeAll
	static void createApplicationUser() throws SQLException
	{
		String adminUrl = "jdbc:oracle:thin:@" + ORACLE.getHost() + ":" + ORACLE.getOraclePort() + "/FREEPDB1";
		try (Connection c = DriverManager.getConnection(adminUrl, "system", SYS_PASSWORD); Statement s = c.createStatement())
		{
			s.execute("CREATE USER " + USER + " IDENTIFIED BY \"" + PASSWORD + "\"");
			s.execute("GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO " + USER);
		}
	}

	@ParameterizedTest(name = "oracle migrations apply cleanly: {0}")
	@ValueSource(strings = {"default", "strict"})
	void migrationsApplyCleanly(String variant)
	{
		Flyway flyway = Flyway.configure()
				.dataSource(ORACLE.getJdbcUrl(), ORACLE.getUsername(), ORACLE.getPassword())
				.locations("classpath:db/migration/" + variant)
				.cleanDisabled(false)
				.load();
		flyway.clean();
		MigrateResult result = flyway.migrate();
		assertThat(result.success).as("flyway.migrate() success for variant '%s'", variant).isTrue();
		assertThat(result.migrationsExecuted).as("migrations executed for variant '%s'", variant).isPositive();
	}
}
