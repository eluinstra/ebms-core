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
package nl.clockwork.ebms.cli.prompt;

import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import nl.clockwork.ebms.cli.properties.ServerDatabase;
import org.jline.prompt.Prompter;
import org.jline.reader.UserInterruptException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerDatabaseSection
{
	public static void prompt(Prompter prompter, ServerDatabase properties) throws UserInterruptException, IOException
	{
		properties.setStart(Prompts.confirm(prompter, "databaseStart", "Start in-process H2 database server?", properties.isStart()));
		properties.setDir(Prompts.input(prompter, "databaseDir", "Database data directory", properties.getDir()));
	}
}
