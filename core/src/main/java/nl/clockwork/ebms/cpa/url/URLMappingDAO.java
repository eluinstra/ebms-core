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
package nl.clockwork.ebms.cpa.url;

import java.util.List;
import java.util.Optional;

public interface URLMappingDAO
{
	void clearCache();

	boolean existsURLMapping(String source);

	Optional<String> getURLMapping(String source);

	List<URLMapping> getURLMappings();

	String insertURLMapping(URLMapping urlMapping);

	int updateURLMapping(URLMapping urlMapping);

	int deleteURLMapping(String source);
}