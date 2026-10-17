/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms.migrate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * The result of migrating an EbMS Admin 2.20.x command line to the Spring properties of EbMS Core/Server 2.21.x.
 *
 * @param properties the migrated properties in stable (grouped) order, ready to be written to an {@code ebms-server.properties} file.
 * @param ignoredOptions the 2.20 option names that were present on the command line but have no 2.21 counterpart.
 * @param warnings human-readable notes about options that were dropped or that require manual follow-up.
 */
@Getter
public class MigrateResult
{
	private final Map<String, String> properties;
	private final List<String> ignoredOptions;
	private final List<String> warnings;

	public MigrateResult(Map<String, String> properties, List<String> ignoredOptions, List<String> warnings)
	{
		this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
		this.ignoredOptions = List.copyOf(ignoredOptions);
		this.warnings = List.copyOf(warnings);
	}

	public static MigrateResult of(Map<String, String> properties)
	{
		return new MigrateResult(properties, new ArrayList<>(), new ArrayList<>());
	}

	public boolean hasMappings()
	{
		return !properties.isEmpty();
	}
}
