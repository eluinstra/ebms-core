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

import java.util.Map;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.jline.prompt.CheckboxResult;
import org.jline.prompt.ConfirmResult;
import org.jline.prompt.EditorResult;
import org.jline.prompt.InputResult;
import org.jline.prompt.KeyPressResult;
import org.jline.prompt.ListResult;
import org.jline.prompt.Prompt;
import org.jline.prompt.PromptResult;
import org.jline.prompt.SearchResult;
import org.jline.prompt.ToggleResult;

@NoArgsConstructor
public class ResultHelper
{

	public static Set<String> getCheckbox(final Map<String, ? extends PromptResult<? extends Prompt>> results, String key)
	{
		return ((CheckboxResult)results.get(key)).getSelectedIds();
	}

	public static boolean getConfirm(final Map<String, ? extends PromptResult<? extends Prompt>> results, String key)
	{
		return ((ConfirmResult)results.get(key)).isConfirmed();
	}

	public static String getEditor(final Map<String, ? extends PromptResult<? extends Prompt>> results, String key)
	{
		return ((EditorResult)results.get(key)).getText();
	}

	public static String getInput(final Map<String, ? extends PromptResult<? extends Prompt>> results, String key)
	{
		return ((InputResult)results.get(key)).getInput();
	}

	public static String getKeypress(final Map<String, ? extends PromptResult<? extends Prompt>> results, String key)
	{
		return ((KeyPressResult)results.get(key)).getKey();
	}

	public static String getList(final Map<String, ? extends PromptResult<? extends Prompt>> results, String key)
	{
		return ((ListResult)results.get(key)).getSelectedId();
	}

	public static String getSearch(final Map<String, ? extends PromptResult<? extends Prompt>> results, String key)
	{
		return ((SearchResult)results.get(key)).getSelectedValue();
	}

	public static boolean getToggle(final Map<String, ? extends PromptResult<? extends Prompt>> results, String key)
	{
		return ((ToggleResult)results.get(key)).isActive();
	}
}
