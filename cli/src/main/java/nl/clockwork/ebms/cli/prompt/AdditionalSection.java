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
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jline.prompt.Prompter;
import org.jline.reader.UserInterruptException;

/**
 * Free-form key/value pairs for properties that are not (yet) modeled as their own prompt. Every existing key that is not covered by the curated sections is
 * preserved by default; re-enter a key to change or remove its value, and add new keys on the fly.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AdditionalSection
{
	public static void prompt(Prompter prompter, Map<String, String> additionalProperties) throws UserInterruptException, IOException
	{
		if (!additionalProperties.isEmpty())
		{
			System.out.println("Additional properties (preserved unless re-entered):");
			additionalProperties.forEach((key, value) -> System.out.println("  " + key + " = " + value));
		}
		while (true)
		{
			val key = Prompts.input(prompter, "additionalKey", "Property key to add/change (empty to finish)", "");
			if (StringUtils.isBlank(key))
				break;
			val value = Prompts.input(prompter, "additionalValue", "Value for " + key + " (empty to remove)", additionalProperties.get(key));
			if (StringUtils.isBlank(value))
				additionalProperties.remove(key);
			else
				additionalProperties.put(key, value);
		}
	}
}
