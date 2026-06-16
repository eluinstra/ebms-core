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
package nl.clockwork.ebms.server.embedded.db;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.server.embedded.config.DBConfig;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
public class DBExecute
{
	public static void main(String[] args) throws Exception
	{
		val options = createOptions();
		val cmd = new DefaultParser().parse(options, args);
		if (cmd.hasOption("h"))
			printUsage(options);

		try (val context = new AnnotationConfigApplicationContext(DBConfig.class))
		{
			if (!StringUtils.isEmpty(context.getEnvironment().getProperty("ebms.jdbc.password")))
				throw new IllegalStateException("Only works for jdbc connections without a password");
			val dbExecute = createDBExecute(context);
			if (!dbExecute.execute(cmd))
				printUsage(options);
		}
		System.exit(0);
	}

	protected static Options createOptions()
	{
		val result = new Options();
		result.addOption("h", false, "print this message");
		result.addOption("file", true, "path to database script file");
		return result;
	}

	private static void printUsage(Options options)
	{
		try
		{
			HelpFormatter.builder().get().printHelp("DBExecute", null, options, null, true);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		System.exit(0);
	}

	private static DBExecute createDBExecute(AnnotationConfigApplicationContext context)
	{
		val dataSource = context.getBean(DataSource.class);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new DBExecute(jdbcTemplate);
	}

	@NonNull
	JdbcTemplate jdbcTemplate;

	private boolean execute(CommandLine cmd) throws DataAccessException, IOException
	{
		if (!cmd.hasOption("file"))
			return false;
		val file = Paths.get(cmd.getOptionValue("file")).toFile();
		val sql = java.util.Objects.requireNonNull(FileUtils.readFileToString(file, Charset.defaultCharset()));
		jdbcTemplate.execute(sql);
		return true;
	}
}
