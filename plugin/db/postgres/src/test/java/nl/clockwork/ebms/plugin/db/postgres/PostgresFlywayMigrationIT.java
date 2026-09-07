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
package nl.clockwork.ebms.plugin.db.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validates the Flyway migrations under {@code src/main/resources/db/migration/<variant>} by applying them to a real PostgreSQL container started on demand via
 * Testcontainers' JDBC URL support ({@code jdbc:tc:postgresql:...}). Replaces the former {@code validate-migrations.sh} harness for this plugin.
 */
class PostgresFlywayMigrationIT
{
	private static final String JDBC_URL = "jdbc:tc:postgresql:16-alpine:///ebms";
	private static final String USER = "ebms";
	private static final String PASSWORD = "ebms";

	@ParameterizedTest(name = "postgres migrations apply cleanly: {0}")
	@ValueSource(strings = {"default", "strict"})
	void migrationsApplyCleanly(String variant)
	{
		Flyway flyway = Flyway.configure().dataSource(JDBC_URL, USER, PASSWORD).locations("classpath:db/migration/" + variant).cleanDisabled(false).load();
		flyway.clean();
		MigrateResult result = flyway.migrate();
		assertThat(result.success).as("flyway.migrate() success for variant '%s'", variant).isTrue();
		assertThat(result.migrationsExecuted).as("migrations executed for variant '%s'", variant).isPositive();
	}
}
