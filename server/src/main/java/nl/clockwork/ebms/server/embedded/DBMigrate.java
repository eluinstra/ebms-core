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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;

@Slf4j
public class DBMigrate
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@Getter
	@ToString
	enum BaselineVersion
	{
		VERSION_2_10("2.10", "2.10.0"),
		VERSION_2_11("2.11", "2.11.0"),
		VERSION_2_12("2.12", "2.12.0"),
		VERSION_2_13("2.13", "2.13.0"),
		VERSION_2_14("2.14", "2.13.0"),
		VERSION_2_15("2.15", "2.15.0"),
		VERSION_2_16("2.16", "2.15.0"),
		VERSION_2_17("2.17", "2.17.0"),
		VERSION_2_18("2.18", "2.18.0");

		String ebmsVersion;
		String migrationBaselineVersion;

		public static Optional<String> getBaselineVersion(String ebmsVersion)
		{
			return Arrays.stream(values()).filter(v -> ebmsVersion.startsWith(v.ebmsVersion)).map(v -> v.migrationBaselineVersion).findFirst();
		}
	}

	public static void main(String[] args) throws ParseException
	{
		val options = createOptions();
		val cmd = new DefaultParser().parse(options, args);
		if (cmd.hasOption("h"))
			printUsage(options);

		migrate(cmd);
	}

	protected static Options createOptions()
	{
		val result = new Options();
		result.addOption("h", false, "print this message");
		result.addOption("jdbcUrl", true, "set jdbcUrl");
		result.addOption("username", true, "set username");
		result.addOption("password", true, "set password");
		result.addOption("strict", false, "use strict db scripts (default: false)");
		result.addOption("ebmsVersion", true, "set current ebmsVersion (default: none)");
		return result;
	}

	protected static void printUsage(Options options)
	{
		try
		{
			HelpFormatter.builder().get().printHelp("DBMigrate", null, options, null, true);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		val versions = Arrays.stream(BaselineVersion.values()).map(v -> v.ebmsVersion).collect(Collectors.joining("\n"));
		log.info("\nValid ebmsVersions:\n{}", versions);
		System.exit(0);
	}

	private static void migrate(CommandLine cmd) throws ParseException
	{
		val jdbcUrl = cmd.getOptionValue("jdbcUrl");
		val username = cmd.getOptionValue("username");
		val password = cmd.getOptionValue("password");
		val isStrict = "true".equals(cmd.getOptionValue("strict"));
		val location = isStrict ? "classpath:db/migration/strict/" : "classpath:db/migration/default/";
		val baselineVersion = parseBaselineVersion(cmd.getOptionValue("ebmsVersion"));
		val config = StringUtils.isNotEmpty(baselineVersion)
				? Flyway.configure()
						.dataSource(jdbcUrl, username, password)
						.locations(location)
						.ignoreMigrationPatterns("*:missing")
						.outOfOrder(true)
						.baselineVersion(baselineVersion)
						.baselineOnMigrate(true)
				: Flyway.configure().dataSource(jdbcUrl, username, password).locations(location).ignoreMigrationPatterns("*:missing").outOfOrder(true);
		log.info("Migration starting...");
		config.load().migrate();
		log.info("Migration finished");
	}

	private static String parseBaselineVersion(String ebmsVersion) throws ParseException
	{
		return StringUtils.isNotEmpty(ebmsVersion)
				? BaselineVersion.getBaselineVersion(ebmsVersion).orElseThrow(() -> new ParseException("ebmsVersion " + ebmsVersion + " not found!"))
				: null;
	}

}
