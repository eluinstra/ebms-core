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

import java.io.File;
import java.nio.file.Path;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EbMSMigratorTest
{
	@TempDir
	Path tmpDir;

	@Test
	void shouldMigrateAdminOptionsToPropertiesFile() throws Exception
	{
		val output = tmpDir.resolve("ebms-server.properties").toFile();
		val exitCode =
				new EbMSMigrator().run(new String[]{"--host", "0.0.0.0", "--port", "9090", "--ssl", "--keyStorePassword", "secret", "-o", output.getAbsolutePath()});
		assertThat(exitCode).isZero();
		assertThat(output).exists();
		val properties = ServerPropertiesFile.read(output);
		assertThat(properties).containsEntry("api.port", "9090")
				.containsEntry("api.ssl.enabled", "true")
				.containsEntry("api.ssl.keyStorePassword", "secret")
				.containsEntry("ebms.host", "0.0.0.0")
				.containsEntry("ebms.ssl", "true")
				.containsEntry("keystore.password", "secret");
	}

	@Test
	void shouldMergeWithExistingPropertiesFile() throws Exception
	{
		val output = tmpDir.resolve("ebms-server.properties").toFile();
		val existing = new java.util.TreeMap<String, String>();
		existing.put("ebms.jdbc.driverClassName", "org.postgresql.Driver");
		existing.put("ebms.jdbc.url", "jdbc:postgresql://db/ebms");
		ServerPropertiesFile.write(output, existing);
		val exitCode = new EbMSMigrator().run(new String[]{"--health", "-o", output.getAbsolutePath()});
		assertThat(exitCode).isZero();
		val properties = ServerPropertiesFile.read(output);
		assertThat(properties).containsEntry("ebms.jdbc.url", "jdbc:postgresql://db/ebms").containsEntry("api.health.enabled", "true");
	}

	@Test
	void shouldFailWithoutAdminOptions()
	{
		assertThat(new EbMSMigrator().run(new String[]{"-o", new File(tmpDir.toFile(), "x.properties").getAbsolutePath()})).isOne();
	}

	@Test
	void shouldFailOnUnknownOption()
	{
		assertThat(new EbMSMigrator().run(new String[]{"--no-such-option"})).isOne();
	}
}
