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
package nl.clockwork.ebms.plugin.db.h2;

import static nl.clockwork.ebms.ThrowingFunction.unchecked;
import static org.h2.tools.Server.createTcpServer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Scanner;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Slf4j
@Configuration
@PropertySource(value = {"classpath:nl/clockwork/ebms/plugin/db/h2/default.properties"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class H2Config
{
	@Bean(name = "databaseServer")
	@Conditional(StartDatabaseServerType.class)
	public org.h2.tools.Server startH2DBServer(
			@Value("${database.start:false}") boolean startDatabase,
			@Value("${database.dir:./h2}") String databaseDir,
			@Value("${ebms.jdbc.driverClassName}") String driverClassName,
			@Value("${ebms.jdbc.url}") String jdbcUrl) throws IOException, URISyntaxException, SQLException
	{
		if (startDatabase)
		{
			return getH2JdbcUrl(driverClassName, jdbcUrl)
					.map(unchecked(url -> createTcpServer("-baseDir", databaseDir, "-ifNotExists", "-tcp", "-tcpPort", url.getPort().toString())))
					.stream()
					.peek(server -> log.info("Starting H2DB Server on port {} in baseDir {}", server.getPort(), databaseDir))
					.findFirst()
					.map(unchecked(Server::start))
					.orElse(null);
		}
		else
			return null;
	}

	private Optional<JdbcURL> getH2JdbcUrl(String driverClassName, String jdbcUrl) throws IOException
	{
		if (driverClassName.startsWith("org.h2"))
		{
			val result = parseJdbcURL(jdbcUrl, new JdbcURL());
			val allowedHosts = "localhost|127.0.0.1";
			if (!result.getHost().matches("^(" + allowedHosts + ")$"))
				throw new IllegalStateException(String.format("Cannot start H2DB Server on %s. Use %s instead.", result.getHost(), allowedHosts));
			return Optional.of(result);
		}
		return Optional.empty();
	}

	private JdbcURL parseJdbcURL(String jdbcURL, JdbcURL model) throws MalformedURLException
	{
		try (val scanner = new Scanner(jdbcURL))
		{
			val protocol = scanner.findInLine("(://|@|:@//)");
			if (protocol != null)
			{
				val urlString = scanner.findInLine("[^/:]+(:\\d+){0,1}");
				scanner.findInLine("(/|:|;databaseName=)");
				val database = scanner.findInLine("[^;]*");
				if (urlString != null)
				{
					val url = new URL("http://" + urlString);
					model.setHost(url.getHost());
					model.setPort(url.getPort() == -1 ? null : url.getPort());
					model.setDatabase(database);
				}
			}
			return model;
		}
	}

	public static class StartDatabaseServerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("database.start", Boolean.class, false);
		}
	}
}
