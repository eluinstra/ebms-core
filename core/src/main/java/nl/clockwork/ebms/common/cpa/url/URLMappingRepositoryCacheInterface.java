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
package nl.clockwork.ebms.common.cpa.url;

import java.util.List;
import java.util.Optional;
import nl.clockwork.ebms.client.delivery.task.URLMappingRepository;

public interface URLMappingRepositoryCacheInterface extends nl.clockwork.ebms.api.cpa.url.URLMappingRepository, URLMappingRepository
{

	void clearCache();

	boolean existsURLMapping(String source);

	String getURL(String source);

	Optional<String> getURLMapping(String source);

	List<URLMapping> getURLMappings();

	void setURLMapping(URLMapping urlMapping);

	String insertURLMapping(URLMapping urlMapping);

	int updateURLMapping(URLMapping urlMapping);

	int deleteURLMapping(String source);

}