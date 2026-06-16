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
package nl.clockwork.ebms.server.embedded.admin;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RealmFileMigrator
{
	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int MAX_PASSWORD_LENGTH = 128;

	public static void main(String[] args) throws Exception
	{
		val exitCode = execute(args, System.out, System.err);
		if (exitCode != 0)
			System.exit(exitCode);
	}

	static int execute(String[] args, PrintStream out, PrintStream err) throws Exception
	{
		try
		{
			val options = createOptions();
			val cmd = new DefaultParser().parse(options, args);
			if (cmd.hasOption("h") || cmd.hasOption("help"))
			{
				printUsage(options);
				return 0;
			}

			validateCommand(cmd);
			val realmFile = Path.of(cmd.getOptionValue("file"));
			if (!Files.exists(realmFile))
				throw new IllegalArgumentException("Realm file does not exist: " + realmFile);

			val operation = getOperation(cmd);
			switch (operation)
			{
				case MIGRATE:
					return migrateUsers(realmFile, out, err);
				case ADD:
					return addUser(realmFile, cmd.getOptionValue("username"), cmd.getOptionValue("password"), cmd.getOptionValue("role", "user"), out);
				case UPDATE:
					return updateUser(realmFile, cmd.getOptionValue("username"), cmd.getOptionValue("password"), cmd.getOptionValue("role"), out);
				case REMOVE:
					return removeUser(realmFile, cmd.getOptionValue("username"), out);
				default:
					throw new IllegalStateException("Unsupported operation");
			}
		}
		catch (IllegalArgumentException e)
		{
			err.println(e.getMessage());
			return 2;
		}
		catch (ParseException e)
		{
			err.println(e.getMessage());
			return 2;
		}
	}

	private static int migrateUsers(Path realmFile, PrintStream out, PrintStream err) throws IOException
	{
		val lines = Files.readAllLines(realmFile, Charset.defaultCharset());
		val migrated = new ArrayList<String>(lines.size());
		val unsupportedUsers = new ArrayList<String>();
		for (val line : lines)
			migrated.add(migrateLine(line, unsupportedUsers));

		writeWithBackup(realmFile, migrated);
		out.println("Backup written to: " + Path.of(realmFile + ".bak"));
		out.println("Migrated realm file written to: " + realmFile);
		if (!unsupportedUsers.isEmpty())
		{
			err.println("Some users could not be auto-migrated (legacy hash format): " + String.join(", ", unsupportedUsers));
			err.println("Reset those passwords manually to BCrypt.");
			return 2;
		}
		return 0;
	}

	private static int addUser(Path realmFile, String username, String password, String role, PrintStream out) throws IOException
	{
		validateUsername(username);
		validatePassword(password);
		validateRole(role);

		val lines = Files.readAllLines(realmFile, Charset.defaultCharset());
		val entries = parseEntries(lines);
		if (findEntryIndex(entries, username) >= 0)
			throw new IllegalArgumentException("User already exists: " + username);

		entries.add(RealmEntry.user(username, BCrypt.hashpw(password, BCrypt.gensalt()), role));
		writeWithBackup(realmFile, toLines(entries));
		out.println("Backup written to: " + Path.of(realmFile + ".bak"));
		out.println("User added: " + username);
		return 0;
	}

	private static int updateUser(Path realmFile, String username, String password, String role, PrintStream out) throws IOException
	{
		validateUsername(username);
		validatePassword(password);
		if (role != null)
			validateRole(role);

		val lines = Files.readAllLines(realmFile, Charset.defaultCharset());
		val entries = parseEntries(lines);
		val index = findEntryIndex(entries, username);
		if (index < 0)
			throw new IllegalArgumentException("User not found: " + username);

		val current = entries.get(index);
		entries.set(index, RealmEntry.user(username, BCrypt.hashpw(password, BCrypt.gensalt()), role == null ? current.role : role));
		writeWithBackup(realmFile, toLines(entries));
		out.println("Backup written to: " + Path.of(realmFile + ".bak"));
		out.println("User updated: " + username);
		return 0;
	}

	private static int removeUser(Path realmFile, String username, PrintStream out) throws IOException
	{
		validateUsername(username);

		val lines = Files.readAllLines(realmFile, Charset.defaultCharset());
		val entries = parseEntries(lines);
		val index = findEntryIndex(entries, username);
		if (index < 0)
			throw new IllegalArgumentException("User not found: " + username);

		entries.remove(index);
		writeWithBackup(realmFile, toLines(entries));
		out.println("Backup written to: " + Path.of(realmFile + ".bak"));
		out.println("User removed: " + username);
		return 0;
	}

	private static void writeWithBackup(Path realmFile, List<String> lines) throws IOException
	{

		val backupFile = Path.of(realmFile + ".bak");
		Files.copy(realmFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
		Files.write(realmFile, lines, Charset.defaultCharset());
	}

	private static String migrateLine(String line, List<String> unsupportedUsers)
	{
		if (isCommentOrBlank(line))
			return line;

		val parsed = parseUserLine(line);
		if (parsed == null)
			return line;

		val username = parsed.username;
		val stored = parsed.password;
		if (stored.startsWith("$2"))
			return username + ":" + stored + "," + parsed.role;
		if (stored.startsWith("MD5:") || stored.startsWith("OBF:") || stored.startsWith("CRYPT:"))
		{
			unsupportedUsers.add(username);
			return line;
		}

		val bcrypt = BCrypt.hashpw(stored, BCrypt.gensalt());
		return username + ":" + bcrypt + "," + parsed.role;
	}

	private static List<RealmEntry> parseEntries(List<String> lines)
	{
		val result = new ArrayList<RealmEntry>(lines.size());
		for (val line : lines)
		{
			if (isCommentOrBlank(line))
				result.add(RealmEntry.raw(line));
			else
			{
				val parsed = parseUserLine(line);
				result.add(parsed == null ? RealmEntry.raw(line) : parsed);
			}
		}
		return result;
	}

	private static List<String> toLines(List<RealmEntry> entries)
	{
		val result = new ArrayList<String>(entries.size());
		for (val entry : entries)
		{
			if (entry.rawLine != null)
				result.add(entry.rawLine);
			else
				result.add(entry.username + ":" + entry.password + "," + entry.role);
		}
		return result;
	}

	private static int findEntryIndex(List<RealmEntry> entries, String username)
	{
		for (int i = 0; i < entries.size(); i++)
		{
			val entry = entries.get(i);
			if (entry.username != null && entry.username.equals(username))
				return i;
		}
		return -1;
	}

	private static RealmEntry parseUserLine(String line)
	{
		val parts = StringUtils.split(line, ",");
		if (parts == null || parts.length != 2)
			return null;

		val userPart = StringUtils.trim(parts[0]);
		val rolePart = StringUtils.trim(parts[1]);
		if (StringUtils.isBlank(userPart) || StringUtils.isBlank(rolePart))
			return null;

		val separator = userPart.indexOf(':');
		if (separator < 1 || separator >= userPart.length() - 1)
			return null;

		val username = StringUtils.trim(userPart.substring(0, separator));
		val password = StringUtils.trim(userPart.substring(separator + 1));
		if (StringUtils.isBlank(username) || StringUtils.isBlank(password))
			return null;

		return RealmEntry.user(username, password, rolePart);
	}

	private static boolean isCommentOrBlank(String line)
	{
		return StringUtils.isBlank(line) || line.trim().startsWith("#");
	}

	private static Operation getOperation(CommandLine cmd)
	{
		val count = (cmd.hasOption("migrate") ? 1 : 0)
				+ (cmd.hasOption("add-user") ? 1 : 0)
				+ (cmd.hasOption("update-user") ? 1 : 0)
				+ (cmd.hasOption("remove-user") ? 1 : 0);
		if (count > 1)
			throw new IllegalArgumentException("Choose only one operation: --migrate, --add-user, --update-user, or --remove-user");
		if (cmd.hasOption("add-user"))
			return Operation.ADD;
		if (cmd.hasOption("update-user"))
			return Operation.UPDATE;
		if (cmd.hasOption("remove-user"))
			return Operation.REMOVE;
		return Operation.MIGRATE;
	}

	private static void validateCommand(CommandLine cmd)
	{
		if (!cmd.hasOption("file"))
			throw new IllegalArgumentException("Missing required option: --file");

		val operation = getOperation(cmd);
		switch (operation)
		{
			case MIGRATE:
				if (cmd.hasOption("username") || cmd.hasOption("password") || cmd.hasOption("role"))
					throw new IllegalArgumentException("--username, --password, and --role are not supported with migrate operation");
				return;
			case ADD:
				if (!cmd.hasOption("username"))
					throw new IllegalArgumentException("Missing required option for add: --username");
				if (!cmd.hasOption("password"))
					throw new IllegalArgumentException("Missing required option for add: --password");
				return;
			case UPDATE:
				if (!cmd.hasOption("username"))
					throw new IllegalArgumentException("Missing required option for update: --username");
				if (!cmd.hasOption("password"))
					throw new IllegalArgumentException("Missing required option for update: --password");
				return;
			case REMOVE:
				if (!cmd.hasOption("username"))
					throw new IllegalArgumentException("Missing required option for remove: --username");
				if (cmd.hasOption("password") || cmd.hasOption("role"))
					throw new IllegalArgumentException("--password and --role are not supported with remove operation");
				return;
			default:
				throw new IllegalStateException("Unsupported operation");
		}
	}

	private static void validateUsername(String username)
	{
		if (StringUtils.isBlank(username))
			throw new IllegalArgumentException("Username must not be blank");
		if (StringUtils.containsAny(username, ':', ',', '\r', '\n'))
			throw new IllegalArgumentException("Username contains unsupported characters (: , CR, LF)");
	}

	private static void validatePassword(String password)
	{
		if (StringUtils.isBlank(password))
			throw new IllegalArgumentException("Password must not be blank");
		if (password.length() < MIN_PASSWORD_LENGTH)
			throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		if (password.length() > MAX_PASSWORD_LENGTH)
			throw new IllegalArgumentException("Password must be at most " + MAX_PASSWORD_LENGTH + " characters");
		if (!StringUtils.isAlphanumeric(password))
			throw new IllegalArgumentException("Password must contain only letters and digits");
		if (!StringUtils.containsAny(password, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ") || !StringUtils.containsAny(password, "0123456789"))
			throw new IllegalArgumentException("Password must contain at least one letter and one digit");
	}

	private static void validateRole(String role)
	{
		if (StringUtils.isBlank(role))
			throw new IllegalArgumentException("Role must not be blank");
		if (StringUtils.containsAny(role, ',', '\r', '\n'))
			throw new IllegalArgumentException("Role contains unsupported characters (, CR, LF)");
	}

	private static Options createOptions()
	{
		val result = new Options();
		result.addOption(Option.builder("h").longOpt("help").desc("print this message").get());
		result.addOption(Option.builder("f").longOpt("file").hasArg().desc("path to realm.properties file").get());
		result.addOption(Option.builder().longOpt("migrate").desc("migrate plaintext realm entries to BCrypt").get());
		result.addOption(Option.builder().longOpt("add-user").desc("add a user to the realm file").get());
		result.addOption(Option.builder().longOpt("update-user").desc("update a user password (and optionally role)").get());
		result.addOption(Option.builder().longOpt("remove-user").desc("remove a user from the realm file").get());
		result.addOption(Option.builder("u").longOpt("username").hasArg().desc("username for add/update/remove operations").get());
		result.addOption(Option.builder("p").longOpt("password").hasArg().desc("plaintext password for add/update operations (stored as BCrypt)").get());
		result.addOption(Option.builder("r").longOpt("role").hasArg().desc("role for add operation (default: user), optional role override for update").get());
		return result;
	}

	private static void printUsage(Options options) throws IOException
	{
		HelpFormatter.builder().get().printHelp("RealmFileMigrator", null, options, null, true);
	}

	private enum Operation
	{
		MIGRATE, ADD, UPDATE, REMOVE
	}

	private static class RealmEntry
	{
		String rawLine;
		String username;
		String password;
		String role;

		static RealmEntry raw(String rawLine)
		{
			val result = new RealmEntry();
			result.rawLine = rawLine;
			return result;
		}

		static RealmEntry user(String username, String password, String role)
		{
			val result = new RealmEntry();
			result.username = Objects.requireNonNull(username, "username");
			result.password = Objects.requireNonNull(password, "password");
			result.role = Objects.requireNonNull(role, "role");
			return result;
		}
	}
}
