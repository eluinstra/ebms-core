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

import org.testcontainers.postgresql.PostgreSQLContainer;

public class FixedPostgreSQLContainer extends PostgreSQLContainer
{
	public FixedPostgreSQLContainer()
	{
		super("postgres:18");
		this.withDatabaseName("ebms").withUsername("ebms").withPassword("ebms").withReuse(true);
		this.start();
	}

	@Override
	public void start()
	{
		if (enabledTestContainers())
		{
			super.start();
		}
	}

	private static boolean enabledTestContainers()
	{
		return System.getenv("DISABLE_TEST_CONTAINERS") == null
				|| System.getenv("DISABLE_TEST_CONTAINERS").equals("")
				|| System.getenv("DISABLE_TEST_CONTAINERS").startsWith("n")
				|| System.getenv("DISABLE_TEST_CONTAINERS").startsWith("N");
	}
}
