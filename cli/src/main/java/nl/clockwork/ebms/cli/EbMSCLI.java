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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;

import org.jline.prompt.Prompter;
import org.jline.prompt.PrompterFactory;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import lombok.val;

public class EbMSCLI
{
	Terminal terminal;
	Prompter prompter;

	private EbMSCLI()
	{
		terminal = createTerminal();
		prompter = PrompterFactory.create(terminal);
	}

	public static void main(String[] args) throws UserInterruptException, IOException
	{
		new EbMSCLI().run(args);
	}

	private void run(String[] args) throws UserInterruptException, IOException
	{
		val consoleProperties = getConsoleProperties();
		terminal.writer().println("consoleProperties: " + consoleProperties);
	}

	private Terminal createTerminal()
	{
		try
		{
			return TerminalBuilder.builder().system(true).build();
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private String getConsoleProperties() throws UserInterruptException, IOException
	{
		val consolePropertiesBuilder = prompter.newBuilder();
		consolePropertiesBuilder.createNumberPrompt()
				.name("maxItemsPerPage")
				.message("maxItemsPerPage?")
				.defaultValue("20")
				.min(1d)
				.max(100d)
				.invalidNumberMessage("nummer tussen 1 en 100")
				.addPrompt();
		val consolePropertiesResults = prompter.prompt(Collections.emptyList(), consolePropertiesBuilder.build());
		return ResultHelper.getInput(consolePropertiesResults, "maxItemsPerPage");
	}

}
