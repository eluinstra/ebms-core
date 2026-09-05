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
package nl.clockwork.ebms.cli.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Database settings that the (embedded) server manages itself: whether to start an in-process database server and the location of its data directory.
 */
@Data
@NoArgsConstructor
public class ServerDatabase
{
	boolean start = true;
	@NonNull
	String dir = "./h2";
}
