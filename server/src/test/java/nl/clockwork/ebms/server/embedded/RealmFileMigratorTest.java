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
package nl.clockwork.ebms.server.embedded;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mindrot.jbcrypt.BCrypt;

class RealmFileMigratorTest
{
	@TempDir
	Path tempDir;

	@Test
	void shouldAddUserWithBcryptPassword() throws Exception
	{
		val realmFile = createRealmFile("admin:$2a$10$4WxSkIW6vSzFbA4Q9x0Q2.u5qbV6elqoc8rKTHMnh6CQ2CsAwM4j.,user");
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice", "--password", "secret123", "--role", "admin");

		assertEquals(0, result.exitCode);
		val lines = Files.readAllLines(realmFile, StandardCharsets.UTF_8);
		assertEquals(2, lines.size());
		assertTrue(lines.get(1).startsWith("alice:$2"));
		assertTrue(lines.get(1).endsWith(",admin"));
		assertTrue(BCrypt.checkpw("secret123", password(lines.get(1))));
		assertTrue(Files.exists(Path.of(realmFile + ".bak")));
	}

	@Test
	void shouldFailAddingExistingUser() throws Exception
	{
		val realmFile = createRealmFile("alice:plain,user");
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice", "--password", "secret123");

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("User already exists: alice"));
	}

	@Test
	void shouldFailAddingUserWithInvalidUsernameCharacters() throws Exception
	{
		val realmFile = createRealmFile("admin:plain,user");
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice,admin", "--password", "secret123");

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("Username contains unsupported characters"));
	}

	@Test
	void shouldUpdateUserPasswordAndKeepRoleByDefault() throws Exception
	{
		val realmFile = createRealmFile("alice:old,user");
		val result = execute("--file", realmFile.toString(), "--update-user", "--username", "alice", "--password", "newsecret1");

		assertEquals(0, result.exitCode);
		val line = Files.readAllLines(realmFile, StandardCharsets.UTF_8).get(0);
		assertTrue(line.startsWith("alice:$2"));
		assertTrue(line.endsWith(",user"));
		assertTrue(BCrypt.checkpw("newsecret1", password(line)));
	}

	@Test
	void shouldFailUpdatingMissingUser() throws Exception
	{
		val realmFile = createRealmFile("admin:secret,user");
		val result = execute("--file", realmFile.toString(), "--update-user", "--username", "ghost", "--password", "secret123");

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("User not found: ghost"));
	}

	@Test
	void shouldFailAddingUserWithShortPassword() throws Exception
	{
		val realmFile = createRealmFile("admin:plain,user");
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice", "--password", "short");

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("Password must be at least 8 characters"));
	}

	@Test
	void shouldAllowAddingUserWithPasswordAtMinimumLength() throws Exception
	{
		val realmFile = createRealmFile("admin:plain,user");
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice", "--password", "abcde123");

		assertEquals(0, result.exitCode);
		val line = Files.readAllLines(realmFile, StandardCharsets.UTF_8).get(1);
		assertTrue(BCrypt.checkpw("abcde123", password(line)));
	}

	@Test
	void shouldFailAddingUserWithPasswordThatIsTooLong() throws Exception
	{
		val realmFile = createRealmFile("admin:plain,user");
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice", "--password", "a1".repeat(65));

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("Password must be at most 128 characters"));
	}

	@Test
	void shouldAllowAddingUserWithPasswordAtMaximumLength() throws Exception
	{
		val realmFile = createRealmFile("admin:plain,user");
		val maxPassword = "a1".repeat(64);
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice", "--password", maxPassword);

		assertEquals(0, result.exitCode);
		val line = Files.readAllLines(realmFile, StandardCharsets.UTF_8).get(1);
		assertTrue(BCrypt.checkpw(maxPassword, password(line)));
	}

	@Test
	void shouldFailUpdatingUserWithInvalidRoleCharacters() throws Exception
	{
		val realmFile = createRealmFile("alice:old,user");
		val result = execute("--file", realmFile.toString(), "--update-user", "--username", "alice", "--password", "newsecret1", "--role", "admin,ops");

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("Role contains unsupported characters"));
	}

	@Test
	void shouldFailAddingUserWithoutDigitInPassword() throws Exception
	{
		val realmFile = createRealmFile("admin:plain,user");
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice", "--password", "password");

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("Password must contain at least one letter and one digit"));
	}

	@Test
	void shouldFailAddingUserWithNonAlphanumericPassword() throws Exception
	{
		val realmFile = createRealmFile("admin:plain,user");
		val result = execute("--file", realmFile.toString(), "--add-user", "--username", "alice", "--password", "secret-123");

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("Password must contain only letters and digits"));
	}

	@Test
	void shouldRemoveUser() throws Exception
	{
		val realmFile = createRealmFile("admin:secret,user", "alice:secret,user");
		val result = execute("--file", realmFile.toString(), "--remove-user", "--username", "alice");

		assertEquals(0, result.exitCode);
		val lines = Files.readAllLines(realmFile, StandardCharsets.UTF_8);
		assertEquals(List.of("admin:secret,user"), lines);
		assertTrue(Files.exists(Path.of(realmFile + ".bak")));
	}

	@Test
	void shouldFailRemovingMissingUser() throws Exception
	{
		val realmFile = createRealmFile("admin:secret,user");
		val result = execute("--file", realmFile.toString(), "--remove-user", "--username", "ghost");

		assertEquals(2, result.exitCode);
		assertTrue(result.err.contains("User not found: ghost"));
	}

	@Test
	void shouldKeepLegacyHashesDuringMigrateAndReturnWarningExitCode() throws Exception
	{
		val realmFile = createRealmFile("legacy:MD5:abc,user", "plain:secret,user");
		val result = execute("--file", realmFile.toString());

		assertEquals(2, result.exitCode);
		val lines = Files.readAllLines(realmFile, StandardCharsets.UTF_8);
		assertEquals("legacy:MD5:abc,user", lines.get(0));
		assertTrue(lines.get(1).startsWith("plain:$2"));
		assertTrue(result.err.contains("legacy hash format"));
	}

	private TestResult execute(String...args) throws Exception
	{
		val out = new ByteArrayOutputStream();
		val err = new ByteArrayOutputStream();
		val exitCode = RealmFileMigrator.execute(args, new PrintStream(out), new PrintStream(err));
		return new TestResult(exitCode, err.toString(StandardCharsets.UTF_8));
	}

	private Path createRealmFile(String...lines) throws Exception
	{
		val realmFile = tempDir.resolve("realm.properties");
		Files.write(realmFile, List.of(lines), StandardCharsets.UTF_8);
		return realmFile;
	}

	private String password(String userLine)
	{
		val first = userLine.indexOf(':');
		val comma = userLine.lastIndexOf(',');
		return userLine.substring(first + 1, comma);
	}

	private static class TestResult
	{
		int exitCode;
		String err;

		TestResult(int exitCode, String err)
		{
			this.exitCode = exitCode;
			this.err = err;
		}
	}
}
