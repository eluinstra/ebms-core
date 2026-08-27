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
package nl.clockwork.ebms.plugin.db.db2;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validates the Flyway migrations under {@code src/main/resources/db/migration/<variant>} by applying them to a real DB2 container started on demand via
 * Testcontainers' JDBC URL support ({@code jdbc:tc:db2:...}). Replaces the former {@code validate-migrations.sh} harness for this plugin.
 * <p>
 * Gated on the host CPU architecture: the DB2 image is amd64-only and would otherwise run under QEMU emulation on arm64 (several minutes to start), so the test
 * is skipped on non-amd64 hosts. The match covers both Linux ({@code amd64}) and macOS ({@code x86_64}) reporting of {@code os.arch}.
 */
@EnabledIfSystemProperty(named = "os.arch", matches = "amd64|x86_64")
class Db2FlywayMigrationIT
{
	private static final String JDBC_URL = "jdbc:tc:db2:11.5.0.0a:///test";
	private static final String USER = "test";
	private static final String PASSWORD = "test";

	@ParameterizedTest(name = "db2 migrations apply cleanly: {0}")
	@ValueSource(strings = {"default"/* FIXME, "strict"*/})
	void migrationsApplyCleanly(String variant)
	{
		Flyway flyway = Flyway.configure().dataSource(JDBC_URL, USER, PASSWORD).locations("classpath:db/migration/" + variant).cleanDisabled(false).load();
		flyway.clean();
		MigrateResult result = flyway.migrate();
		assertThat(result.success).as("flyway.migrate() success for variant '%s'", variant).isTrue();
		assertThat(result.migrationsExecuted).as("migrations executed for variant '%s'", variant).isPositive();
	}
}
