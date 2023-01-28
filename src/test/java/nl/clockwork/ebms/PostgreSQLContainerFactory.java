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
package nl.clockwork.ebms;


import io.restassured.RestAssured;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostgreSQLContainerFactory
{

	public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext)
		{
			RestAssured.port = 8080;
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
					"ebms.jdbc.driverClassName=" + container.getDriverClassName(),
					"ebms.jdbc.url=" + container.getJdbcUrl(),
					"ebms.jdbc.username=" + container.getUsername(),
					"ebms.jdbc.password=" + container.getPassword());
		}
	}

	private static PostgreSQLContainer<?> container = new PostgreSQLContainer("postgres:15.1")
	{
		public void stop()
		{
			// do nothing
		}
	}.withDatabaseName("ebms").withUsername("ebms").withPassword("ebms");

	public static PostgreSQLContainer<?> get()
	{
		return container;
	}
}
