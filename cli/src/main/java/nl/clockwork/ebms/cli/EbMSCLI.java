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
package nl.clockwork.ebms.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.cli.prompt.AdditionalSection;
import nl.clockwork.ebms.cli.prompt.ConsoleSection;
import nl.clockwork.ebms.cli.prompt.CoreSection;
import nl.clockwork.ebms.cli.prompt.EncryptionSection;
import nl.clockwork.ebms.cli.prompt.HttpSection;
import nl.clockwork.ebms.cli.prompt.JdbcSection;
import nl.clockwork.ebms.cli.prompt.Prompts;
import nl.clockwork.ebms.cli.prompt.ServerDatabaseSection;
import nl.clockwork.ebms.cli.prompt.ServiceSection;
import nl.clockwork.ebms.cli.prompt.SignatureSection;
import nl.clockwork.ebms.cli.properties.AdminProperties;
import nl.clockwork.ebms.cli.properties.AdminPropertiesReader;
import nl.clockwork.ebms.cli.properties.AdminPropertiesWriter;
import nl.clockwork.ebms.cli.properties.PropertiesType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jline.prompt.Prompter;
import org.jline.prompt.PrompterFactory;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Command line interface to create and edit ebms-admin properties files.
 *
 * <pre>
 * ebms-cli create [EBMS_ADMIN|EBMS_ADMIN_EMBEDDED] [-o file]
 * ebms-cli edit &lt;file&gt;
 * </pre>
 */
@Slf4j
public class EbMSCLI
{
	private static final String HELP_OPTION = "h";
	private static final String CREATE_COMMAND = "create";
	private static final String EDIT_COMMAND = "edit";
	private static final String OUTPUT_OPTION = "o";

	private final Terminal terminal;
	private final Prompter prompter;

	public EbMSCLI()
	{
		terminal = createTerminal();
		prompter = PrompterFactory.create(terminal);
	}

	public static void main(String[] args)
	{
		val cli = new EbMSCLI();
		System.exit(cli.run(args));
	}

	int run(String[] args)
	{
		val options = createOptions();
		val parser = new DefaultParser();
		CommandLine cmd;
		try
		{
			cmd = parser.parse(options, args);
		}
		catch (ParseException e)
		{
			println(e.getMessage());
			printUsage(options);
			return 1;
		}
		if (cmd.hasOption(HELP_OPTION) || cmd.getArgList().isEmpty())
		{
			printUsage(options);
			return cmd.hasOption(HELP_OPTION) ? 0 : 1;
		}
		val command = cmd.getArgList().get(0);
		try
		{
			return switch (command)
			{
				case CREATE_COMMAND -> create(cmd);
				case EDIT_COMMAND -> edit(cmd);
				default -> {
					println("Unknown command: " + command);
					printUsage(options);
					yield 1;
				}
			};
		}
		catch (UserInterruptException e)
		{
			println("");
			println("Cancelled, nothing was written.");
			return 130;
		}
		catch (EndOfFileException e)
		{
			// stdin ended (e.g. Ctrl-D) before all prompts were answered: abort without writing
			println("");
			println("Input ended, nothing was written.");
			return 130;
		}
		catch (IOException e)
		{
			log.error("", e);
			println("Error: " + e.getMessage());
			return 1;
		}
	}

	private int create(CommandLine cmd) throws IOException, UserInterruptException
	{
		val propertiesType = promptPropertiesType(cmd.getArgList().size() > 1 ? cmd.getArgList().get(1) : null);
		val file = resolveOutputFile(cmd, propertiesType);
		if (file.exists())
		{
			println(file + " already exists, refusing to overwrite.");
			return 1;
		}
		val adminProperties = new AdminProperties();
		promptSections(propertiesType, adminProperties);
		return write(file, adminProperties, propertiesType);
	}

	private int edit(CommandLine cmd) throws IOException, UserInterruptException
	{
		if (cmd.getArgList().size() < 2)
		{
			println("edit requires a file argument");
			return 1;
		}
		val file = new File(cmd.getArgList().get(1));
		if (!file.exists())
		{
			println("File does not exist: " + file);
			return 1;
		}
		val propertiesType = PropertiesType.getPropertiesTypeOrFail(file.getName());
		AdminProperties adminProperties;
		try (val in = new FileInputStream(file))
		{
			adminProperties = new AdminPropertiesReader(in, propertiesType).read();
		}
		println("Loaded " + file);
		promptSections(propertiesType, adminProperties);
		return write(file, adminProperties, propertiesType);
	}

	private PropertiesType promptPropertiesType(String hint) throws UserInterruptException, IOException
	{
		if (hint != null)
		{
			val propertiesType = resolvePropertiesType(hint);
			System.out.println(propertiesType.getDescription() + " (" + propertiesType.getPropertiesFile() + ")");
			return propertiesType;
		}
		val options = new LinkedHashMap<String, String>();
		Arrays.asList(PropertiesType.values()).forEach(t -> options.put(t.name(), t.getDescription() + " (" + t.getPropertiesFile() + ")"));
		val selected = Prompts.list(prompter, "propertiesType", "Properties type", options, PropertiesType.EBMS_ADMIN_EMBEDDED.name());
		return PropertiesType.valueOf(selected);
	}

	/**
	 * Accepts either the enum name (e.g. {@code EBMS_ADMIN_EMBEDDED}) or the properties file name (e.g. {@code ebms-admin.embedded.properties}).
	 */
	private static PropertiesType resolvePropertiesType(String hint)
	{
		try
		{
			return PropertiesType.valueOf(hint.toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			return PropertiesType.getPropertiesTypeOrFail(hint);
		}
	}

	private File resolveOutputFile(CommandLine cmd, PropertiesType propertiesType)
	{
		String output = cmd.getOptionValue(OUTPUT_OPTION, propertiesType.getPropertiesFile());
		return new File(output);
	}

	private void promptSections(PropertiesType propertiesType, AdminProperties adminProperties) throws UserInterruptException, IOException
	{
		System.out.println();
		System.out.println("Console properties (press Enter to keep the shown value)");
		ConsoleSection.prompt(prompter, adminProperties.getConsoleProperties());
		System.out.println();
		System.out.println("Core properties");
		CoreSection.prompt(prompter, adminProperties.getCoreProperties());
		switch (propertiesType)
		{
			case EBMS_ADMIN:
				System.out.println();
				System.out.println("Service properties");
				ServiceSection.prompt(prompter, adminProperties.getServiceProperties());
				break;
			case EBMS_ADMIN_EMBEDDED:
				System.out.println();
				System.out.println("HTTP properties");
				HttpSection.prompt(prompter, adminProperties.getHttpProperties());
				System.out.println();
				System.out.println("Server database properties");
				ServerDatabaseSection.prompt(prompter, adminProperties.getServerDatabase());
				break;
		}
		System.out.println();
		System.out.println("JDBC properties");
		JdbcSection.prompt(prompter, adminProperties.getJdbcProperties());
		if (PropertiesType.EBMS_ADMIN_EMBEDDED.equals(propertiesType))
		{
			System.out.println();
			System.out.println("Signature properties");
			SignatureSection.prompt(prompter, adminProperties.getSignatureProperties());
			System.out.println();
			System.out.println("Encryption properties");
			EncryptionSection.prompt(prompter, adminProperties.getEncryptionProperties());
		}
		System.out.println();
		System.out.println("Additional properties");
		AdditionalSection.prompt(prompter, adminProperties.getAdditionalProperties());
	}

	private int write(File file, AdminProperties adminProperties, PropertiesType propertiesType) throws IOException, UserInterruptException
	{
		System.out.println();
		System.out.println("Properties will be written to " + file.getAbsolutePath());
		if (!Prompts.confirm(prompter, "save", "Save?", true))
		{
			println("Cancelled, nothing was written.");
			return 130;
		}
		try (val writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
		{
			new AdminPropertiesWriter(writer).write(adminProperties, propertiesType);
		}
		println("Written " + file.getAbsolutePath());
		println("Restart the server to apply the new configuration.");
		return 0;
	}

	private static Options createOptions()
	{
		val result = new Options();
		result.addOption(HELP_OPTION, "help", false, "print this message");
		result.addOption(OUTPUT_OPTION, "output", true, "output file (create, default: <properties file of the selected type> in the current directory)");
		return result;
	}

	private static void printUsage(Options options)
	{
		new HelpFormatter().printHelp(new java.io.PrintWriter(System.out, true), 120, """
				Usage: ebms-cli <command> [options]

				Commands:
				  create [EBMS_ADMIN|EBMS_ADMIN_EMBEDDED]  create a new ebms-admin properties file
				  edit <file>                               edit an existing ebms-admin properties file
				""", "Options:", options, 2, 2, null);
	}

	private static Terminal createTerminal()
	{
		try
		{
			return TerminalBuilder.builder().system(true).build();
		}
		catch (IOException e)
		{
			throw new java.io.UncheckedIOException(e);
		}
	}

	private static void println(String s)
	{
		System.out.println(s);
	}
}
