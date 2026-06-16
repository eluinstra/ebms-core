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
package nl.clockwork.ebms.server.embedded.web;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import nl.clockwork.ebms.server.model.embedded.web.MenuItem;

public abstract class ExtensionProvider
{
	public static List<ExtensionProvider> get()
	{
		return ServiceLoader.load(ExtensionProvider.class).stream().map(Provider::get).toList();
	}

	public abstract Class<?> getSpringConfigurationClass();

	public abstract String getDbMigrationLocation();

	public abstract String getName();

	public abstract MenuItem createSubMenu(MenuItem parent, int index, String name);
}
