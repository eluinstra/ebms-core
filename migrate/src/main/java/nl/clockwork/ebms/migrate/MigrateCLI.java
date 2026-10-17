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
package nl.clockwork.ebms.migrate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command line interface that migrates the command line parameters used to configure EbMS Admin 2.20.x to the Spring properties used by EbMS Core and Server
 * 2.21.x.
 *
 * <pre>
 * ebms-migrate &lt;old 2.20 admin command line args&gt; [-output file] [-dryRun]
 * </pre>
 *
 * The 2.20 args are parsed with the exact option set of the legacy Admin {@code Start} class, translated to the 2.21 property keys and written (or printed) as
 * a Java properties file. The tool's own options ({@code -help}, {@code -output}, {@code -dryRun}) are not part of the 2.20 option set and are separated out
 * before parsing.
 */
public class MigrateCLI
{
	private static final String HELP_OPTION = "help";
	private static final String OUTPUT_OPTION = "output";
	private static final String DRY_RUN_OPTION = "dryRun";
	private static final String DEFAULT_OUTPUT = "ebms-server.properties";
	private static final int USAGE_WIDTH = 120;

	public static void main(String[] args)
	{
		System.exit(new MigrateCLI().run(args));
	}

	int run(String[] args)
	{
		val split = splitArgs(args);
		Options toolOptions = createToolOptions();
		CommandLine toolCmd;
		try
		{
			toolCmd = new DefaultParser().parse(toolOptions, split.toolArgs().toArray(new String[0]));
		}
		catch (ParseException e)
		{
			println(e.getMessage());
			printUsage(toolOptions);
			return 1;
		}
		if (toolCmd.hasOption(HELP_OPTION))
		{
			printUsage(toolOptions);
			return 0;
		}
		if (split.adminArgs().isEmpty())
		{
			println("No 2.20 admin parameters given.");
			printUsage(toolOptions);
			return 1;
		}
		try
		{
			return migrate(split.adminArgs(), toolCmd);
		}
		catch (ParseException e)
		{
			println("Cannot parse the 2.20 admin command line: " + e.getMessage());
			return 1;
		}
		catch (IOException e)
		{
			println("Error: " + e.getMessage());
			return 1;
		}
	}

	private int migrate(List<String> adminArgs, CommandLine toolCmd) throws ParseException, IOException
	{
		val adminCmd = MigrateAdmin20ToServer21.parseAdminCommandLine(adminArgs.toArray(new String[0]));
		val result = new MigrateAdmin20ToServer21(adminCmd).migrate();
		if (!result.hasMappings())
		{
			println("No mappable 2.20 parameters found, nothing to migrate.");
			return 1;
		}
		println("Migrated " + result.getProperties().size() + " properties from the 2.20 admin command line:");
		result.getProperties().forEach((key, value) -> println("  " + key + " = " + value));
		for (String warning : result.getWarnings())
			println("  warning: " + warning);

		if (toolCmd.hasOption(DRY_RUN_OPTION))
		{
			println("Dry run, nothing written.");
			return 0;
		}
		val file = new File(toolCmd.getOptionValue(OUTPUT_OPTION, DEFAULT_OUTPUT));
		if (file.exists() && file.isFile())
		{
			println(file + " already exists, refusing to overwrite.");
			return 1;
		}
		try (val writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
		{
			new ServerPropertiesWriter(result.getProperties()).write(writer);
		}
		println("Written " + file.getAbsolutePath());
		println("Review the file and the warnings above, then start the 2.21 server with it.");
		return 0;
	}

	/**
	 * Separates the tool's own options ({@code -help}, {@code -output}/value, {@code -dryRun}) from the legacy 2.20 admin arguments. The 2.20 option set has no
	 * option named {@code output} or {@code dryRun}, so this split is unambiguous.
	 *
	 * @param toolArgs the tool's own arguments.
	 * @param adminArgs the legacy 2.20 admin arguments.
	 */
	record SplitArgs(List<String> toolArgs, List<String> adminArgs)
	{
	}

	private static SplitArgs splitArgs(String[] args)
	{
		val tool = new ArrayList<String>();
		val admin = new ArrayList<String>();
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.equals("-" + HELP_OPTION) || arg.equals("-" + DRY_RUN_OPTION))
				tool.add(arg);
			else if (arg.equals("-" + OUTPUT_OPTION))
			{
				tool.add(arg);
				if (i + 1 < args.length)
				{
					tool.add(args[++i]);
				}
			}
			else
				admin.add(arg);
		}
		return new SplitArgs(tool, admin);
	}

	private static Options createToolOptions()
	{
		val result = new Options();
		result.addOption(Option.builder().longOpt(HELP_OPTION).desc("print this message").build());
		result.addOption(Option.builder().longOpt(OUTPUT_OPTION).hasArg().argName("file").desc("output properties file [default: " + DEFAULT_OUTPUT + "]").build());
		result.addOption(Option.builder().longOpt(DRY_RUN_OPTION).desc("only print the migrated properties, do not write a file").build());
		return result;
	}

	private static void printUsage(Options options)
	{
		new HelpFormatter().printHelp(new PrintWriter(System.out, true), USAGE_WIDTH, """
				Usage: ebms-migrate <2.20 admin args> [tool options]

				Migrates EbMS Admin 2.20.x command line parameters to the Spring properties of
				EbMS Core/Server 2.21.x. Pass the old 2.20 arguments (the ones used with the
				legacy Start class) plus the tool options.

				Example:
				  ebms-migrate -port 8888 -path /ebms -ssl -keyStorePath /keys/keystore.p12 -keyStorePassword secret -jmx

				Tool options:
				""", "  ", options, 2, 2, null);
	}

	private static void println(String s)
	{
		System.out.println(s);
	}
}
