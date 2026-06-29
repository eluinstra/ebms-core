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
package nl.clockwork.ebms.plugin.db.mssql;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Validates the Flyway migrations under {@code src/main/resources/db/migration/<variant>} by
 * applying them to a real SQL Server container. The {@code jdbc:tc:} URL style is not used here
 * because the official MS SQL Server image is amd64-only; we substitute the multi-arch
 * {@code azure-sql-edge} image instead. Replaces the former {@code validate-migrations.sh}
 * harness for this plugin.
 */
@Testcontainers
class MsSqlFlywayMigrationIT
{
	@Container
	static final MSSQLServerContainer<?> MSSQL = new MSSQLServerContainer<>(
			DockerImageName.parse("mcr.microsoft.com/azure-sql-edge:latest").asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
			.acceptLicense();

	@ParameterizedTest(name = "mssql migrations apply cleanly: {0}")
	@ValueSource(strings = {"default"})
	void migrationsApplyCleanly(String variant)
	{
		Flyway flyway = Flyway.configure()
				.dataSource(MSSQL.getJdbcUrl(), MSSQL.getUsername(), MSSQL.getPassword())
				.locations("classpath:db/migration/" + variant)
				.cleanDisabled(false)
				.load();
		flyway.clean();
		MigrateResult result = flyway.migrate();
		assertThat(result.success).as("flyway.migrate() success for variant '%s'", variant).isTrue();
		assertThat(result.migrationsExecuted).as("migrations executed for variant '%s'", variant).isPositive();
	}
}
