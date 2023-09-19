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

import java.util.Arrays;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Predicates
{
	public static Predicate<String> contains(String...values)
	{
		return obj -> Arrays.stream(values).anyMatch(v -> obj.contains(v));
	}

	public static Predicate<String> startsWith(String value)
	{
		return obj -> obj.startsWith(value);
	}

	public static Predicate<String> endsWith(String value)
	{
		return obj -> obj.endsWith(value);
	}
}
