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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The commandline options of the EbMS Admin 2.20.x startup classes ({@code Start} and {@code StartEmbedded}), parsed with commons-cli.
 */
public class AdminStartupOptions
{
	private static final List<String> ADMIN_ONLY_OPTIONS = List.of("keyStoresType");

	private static final List<String> REMOVED_BOOL_OPTIONS = List.of("clientCertificateHeader", "disableHostnameVerification");

	private static final int HELP_LINE_LENGTH = 140;

	private final CommandLine commandLine;

	private AdminStartupOptions(CommandLine commandLine)
	{
		this.commandLine = commandLine;
	}

	public static AdminStartupOptions parse(String...args) throws ParseException
	{
		return new AdminStartupOptions(new DefaultParser().parse(createOptions(), args));
	}

	public static AdminStartupOptions of(CommandLine commandLine)
	{
		return new AdminStartupOptions(commandLine);
	}

	public static Options createOptions()
	{
		val result = new Options();
		result.addOption(Option.builder("h").longOpt("help").desc("print this message").build());
		result.addOption(hasArg("host", "set host [default: 0.0.0.0]"));
		result.addOption(hasArg("port", "set port [default: <8080|8443>]"));
		result.addOption(hasArg("path", "set path [default: /]"));
		result.addOption(noArg("soap", "start SOAP service"));
		result.addOption(noArg("headless", "start without web interface"));
		result.addOption(noArg("health", "start health service"));
		result.addOption(hasArg("healthPort", "set health service port [default: 8008]"));
		result.addOption(hasArg("connectionLimit", "set connection limit"));
		result.addOption(hasArg("queriesPerSecond", "set max requests per second"));
		result.addOption(hasArg("userQueriesPerSecond", "set max requests per user per second"));
		result.addOption(noArg("auditLogging", "enable audit logging"));
		result.addOption(hasArg("echoHeaderNames", "set echo header names"));
		result.addOption(hasArg("mdcHeaderNames", "set logging MDC header names"));
		result.addOption(noArg("ssl", "enable SSL"));
		result.addOption(noArg("disableHostnameVerification", "disable SSL hostname verification"));
		result.addOption(hasArg("protocols", "set SSL Protocols"));
		result.addOption(hasArg("cipherSuites", "set SSL CipherSuites"));
		result.addOption(hasArg("keyStoresType", "set keystores type"));
		result.addOption(hasArg("keyStoreType", "set keystore type [default: PKCS12]"));
		result.addOption(hasArg("keyStorePath", "set keystore path"));
		result.addOption(hasArg("keyStorePassword", "set keystore password"));
		result.addOption(noArg("clientAuthentication", "enable SSL client authentication"));
		result.addOption(hasArg("clientCertificateHeader", "set client certificate header"));
		result.addOption(hasArg("trustStoreType", "set truststore type [default: PKCS12]"));
		result.addOption(hasArg("trustStorePath", "set truststore path"));
		result.addOption(hasArg("trustStorePassword", "set truststore password"));
		result.addOption(noArg("authentication", "enable basic | client certificate authentication"));
		result.addOption(hasArg("clientTrustStoreType", "set client truststore type [default: PKCS12]"));
		result.addOption(hasArg("clientTrustStorePath", "set client truststore path"));
		result.addOption(hasArg("clientTrustStorePassword", "set client truststore password"));
		result.addOption(hasArg("configDir", "set config directory [default: <startup_directory>]"));
		result.addOption(noArg("jmx", "start JMX server"));
		result.addOption(hasArg("jmxPort", "set JMX port [default: 1999]"));
		result.addOption(hasArg("jmxAccessFile", "set JMX access file"));
		result.addOption(hasArg("jmxPasswordFile", "set JMX password file"));
		result.addOption(noArg("disableEbMSServer", "disable ebms server"));
		result.addOption(noArg("disableEbMSClient", "disable ebms client"));
		return result;
	}

	private static Option hasArg(String opt, String description)
	{
		return Option.builder(opt).longOpt(opt).hasArg().desc(description).build();
	}

	private static Option noArg(String opt, String description)
	{
		return Option.builder(opt).longOpt(opt).desc(description).build();
	}

	public static void printUsage(String appName)
	{
		new HelpFormatter().printHelp(new java.io.PrintWriter(System.out, true), HELP_LINE_LENGTH, appName + """
				          <admin startup options> [-o file]

				  Migrates the EbMS Admin 2.20.x startup options (Start/StartEmbedded) to an
				  ebms-server.properties file for EbMS Core and Server 2.21.x. Pass the options
				  exactly as you used to pass them to ebms-admin; all other settings are read
				  from the existing ebms-server.properties in the current directory.
				""", "Options:", createOptions(), 2, 2, null);
	}

	public boolean hasOption(String opt)
	{
		return commandLine.hasOption(opt);
	}

	public String getOptionValue(String opt)
	{
		return commandLine.getOptionValue(opt);
	}

	public String getOptionValue(String opt, String defaultValue)
	{
		return commandLine.getOptionValue(opt, defaultValue);
	}

	public List<String> unknownOptionNames()
	{
		val result = new ArrayList<String>();
		for (val option : ADMIN_ONLY_OPTIONS)
		{
			if (commandLine.hasOption(option))
				result.add(option);
		}
		return result;
	}

	public List<String> removedOptionNames()
	{
		val result = new ArrayList<String>();
		for (val option : REMOVED_BOOL_OPTIONS)
		{
			if (commandLine.hasOption(option))
				result.add(option);
		}
		return result;
	}

	public Optional<String> movedClientCertificateHeaderValue()
	{
		val value = commandLine.getOptionValue("clientCertificateHeader");
		return value == null ? Optional.empty() : Optional.of(value);
	}

	public List<String> warningLines()
	{
		val result = new ArrayList<String>();
		for (val option : unknownOptionNames())
			result.add("ignored: --" + option + " has no equivalent spring property in 2.21.x");
		for (val option : removedOptionNames())
			result.add("ignored: --" + option + " has been removed from the ebms-server in 2.21.x");
		return result;
	}
}
