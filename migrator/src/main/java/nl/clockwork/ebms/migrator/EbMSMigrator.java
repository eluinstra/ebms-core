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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command line application to migrate the EbMS Admin 2.20.x commandline startup parameters to the spring properties used by EbMS Core and Server 2.21.x.
 *
 * <pre>
 * ebms-migrator [ebms-admin 2.20.x startup options] [-o file]
 * </pre>
 *
 * Pass the options exactly as they were used to start ebms-admin 2.20.x ({@code Start}/{@code StartEmbedded}); the tool writes the equivalent spring properties
 * to an ebms-server.properties file and prints the {@code api.*} settings as {@code -D} vmArgs, since the embedded startup reads those through
 * {@code System.getProperty}. Options without an equivalent spring property are reported as warnings.
 */
@Slf4j
public class EbMSMigrator
{
	private static final String OUTPUT_OPTION = "o";

	public static void main(String[] args)
	{
		System.exit(new EbMSMigrator().run(args));
	}

	int run(String[] args)
	{
		val options = createOptions();
		CommandLine cmd;
		try
		{
			cmd = new DefaultParser().parse(options, args);
		}
		catch (ParseException e)
		{
			println(e.getMessage());
			AdminStartupOptions.printUsage(getSimpleName());
			return 1;
		}
		if (cmd.hasOption('h'))
		{
			AdminStartupOptions.printUsage(getSimpleName());
			return 0;
		}
		val adminOptions = AdminStartupOptions.of(cmd);
		if (adminOptions.unknownOptionNames().isEmpty() && adminOptions.removedOptionNames().isEmpty() && !hasAnyAdminOption(adminOptions))
		{
			println("No EbMS Admin 2.20.x startup options given; nothing to migrate.");
			AdminStartupOptions.printUsage(getSimpleName());
			return 1;
		}
		val output = new File(cmd.getOptionValue(OUTPUT_OPTION, ServerPropertiesFile.EBMS_SERVER_PROPERTIES));
		try
		{
			return migrate(adminOptions, output);
		}
		catch (IOException e)
		{
			log.error("", e);
			println("Error: " + e.getMessage());
			return 1;
		}
	}

	private boolean hasAnyAdminOption(AdminStartupOptions adminOptions)
	{
		for (val option : AdminStartupOptions.createOptions().getOptions())
		{
			if (!"h".equals(option.getOpt()) && adminOptions.hasOption(option.getOpt()))
				return true;
		}
		return false;
	}

	private int migrate(AdminStartupOptions adminOptions, File output) throws IOException
	{
		val migrator = new AdminOptionsMigrator();
		val currentProperties = ServerPropertiesFile.read(output);
		val migratedProperties = migrator.migrate(adminOptions, currentProperties);
		ServerPropertiesFile.write(output, migratedProperties);
		println("Migrated EbMS Admin 2.20.x startup options to " + output.getAbsolutePath() + ":");
		for (val entry : migratedProperties.entrySet())
		{
			if (!entry.getValue().equals(currentProperties.get(entry.getKey())))
				println("  " + entry.getKey() + "=" + entry.getValue());
		}
		val vmArgs = new ArrayList<>(migrator.toVmArguments(migratedProperties));
		if (!vmArgs.isEmpty())
		{
			println("");
			println("The api.* properties are read through system properties by the 2.21.x embedded startup; pass them as vmArgs:");
			println("  java " + String.join(" ", vmArgs) + " -jar ebms-server.jar");
		}
		val warnings = migrator.warnings(adminOptions);
		if (!warnings.isEmpty())
		{
			println("");
			println("Warnings:");
			for (val warning : warnings)
			{
				println("  " + warning);
			}
		}
		println("");
		println("Start the server with nl.clockwork.ebms.server.embedded.startup.StartEmbedded and review " + output.getName() + ".");
		return 0;
	}

	private static Options createOptions()
	{
		val result = AdminStartupOptions.createOptions();
		result.addOption(
				Option.builder(OUTPUT_OPTION)
						.longOpt("output")
						.hasArg()
						.desc("output properties file [default: " + ServerPropertiesFile.EBMS_SERVER_PROPERTIES + "]")
						.build());
		return result;
	}

	private static String getSimpleName()
	{
		return EbMSMigrator.class.getSimpleName().toLowerCase();
	}

	private static void println(String s)
	{
		System.out.println(s);
	}
}
