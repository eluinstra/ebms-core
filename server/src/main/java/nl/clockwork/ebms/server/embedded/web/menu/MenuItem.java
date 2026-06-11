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
package nl.clockwork.ebms.server.embedded.web.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class MenuItem implements Serializable
{
	private static final long serialVersionUID = 1L;
	@NonNull
	String id;
	@NonNull
	String name;
	MenuItem parent;
	List<MenuItem> children = new ArrayList<>();

	public MenuItem(String id, String name)
	{
		this(null, id, name);
	}

	public MenuItem(MenuItem parent, String id, String name)
	{
		this.id = parent == null ? id : parent.getId() + "." + id;
		this.name = name;
		this.parent = parent;
		if (parent != null)
			this.parent.children.add(this);
	}

	public List<MenuItem> getChildren()
	{
		return children;
	}

	@Override
	public String toString()
	{
		return id;
	}
}
