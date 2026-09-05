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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.cli.ResultHelper;
import org.apache.commons.lang3.StringUtils;
import org.jline.prompt.Prompter;
import org.jline.reader.UserInterruptException;

/**
 * Thin helpers around the jline {@link Prompter} for the prompt types used by the CLI. All prompts default to the current value of the model, so on an existing
 * file the user can simply press Enter to keep it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Prompts
{
	public static String input(Prompter prompter, String name, String message, String defaultValue) throws UserInterruptException, IOException
	{
		val builder = prompter.newBuilder();
		builder.createInputPrompt().name(name).message(message).defaultValue(StringUtils.defaultString(defaultValue)).addPrompt();
		val results = prompter.prompt(Collections.emptyList(), builder.build());
		return StringUtils.trimToNull(ResultHelper.getInput(results, name));
	}

	public static String password(Prompter prompter, String name, String message, String defaultValue) throws UserInterruptException, IOException
	{
		val builder = prompter.newBuilder();
		builder.createPasswordPrompt().name(name).message(message).defaultValue(StringUtils.defaultString(defaultValue)).addPrompt();
		val results = prompter.prompt(Collections.emptyList(), builder.build());
		return StringUtils.trimToNull(ResultHelper.getInput(results, name));
	}

	public static Integer number(Prompter prompter, String name, String message, Integer defaultValue, int min, int max)
			throws UserInterruptException, IOException
	{
		val builder = prompter.newBuilder();
		builder.createNumberPrompt()
				.name(name)
				.message(message)
				.defaultValue(defaultValue == null ? "" : defaultValue.toString())
				.min((double)min)
				.max((double)max)
				.invalidNumberMessage("enter a whole number")
				.outOfRangeMessage("number must be between " + min + " and " + max)
				.addPrompt();
		val results = prompter.prompt(Collections.emptyList(), builder.build());
		return Integer.parseInt(ResultHelper.getInput(results, name));
	}

	public static boolean confirm(Prompter prompter, String name, String message, boolean defaultValue) throws UserInterruptException, IOException
	{
		val builder = prompter.newBuilder();
		builder.createConfirmPrompt().name(name).message(message).defaultValue(defaultValue).addPrompt();
		val results = prompter.prompt(Collections.emptyList(), builder.build());
		return ResultHelper.getConfirm(results, name);
	}

	/**
	 * Prompts for a choice from a list of options. The (current) default option is shown first, which jline preselects, so pressing Enter keeps the current
	 * value.
	 *
	 * @param prompter the jline prompter
	 * @param name the prompt name
	 * @param message the prompt message
	 * @param options the available options, option id to display text
	 * @param defaultId the option id to preselect, must be one of the options
	 * @return the selected option id
	 */
	public static String list(Prompter prompter, String name, String message, Map<String, String> options, String defaultId)
			throws UserInterruptException, IOException
	{
		val entries = new ArrayList<>(options.entrySet());
		entries.sort(Comparator.comparingInt(e -> e.getKey().equals(defaultId) ? 0 : 1));
		val builder = prompter.newBuilder();
		val listBuilder = builder.createListPrompt().name(name).message(message);
		for (val entry : entries)
		{
			val suffix = entry.getKey().equals(defaultId) ? " (current)" : "";
			listBuilder.add(entry.getKey(), entry.getValue() + suffix);
		}
		listBuilder.addPrompt();
		val results = prompter.prompt(Collections.emptyList(), builder.build());
		return ResultHelper.getList(results, name);
	}
}
