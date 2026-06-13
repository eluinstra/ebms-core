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
package nl.clockwork.ebms.api.url.soap;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.api.url.repository.URLMappingRepository;
import nl.clockwork.ebms.api.url.rest.URLMappingRestController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class URLMappingControllerConfig
{
	@Bean
	public URLMappingController urlMappingService(URLMappingRepository urlMappingRepository)
	{
		return new URLMappingControllerImpl(urlMappingRepository);
	}

	@Bean
	public URLMappingRestController urlMappingRestService(URLMappingController urlMappingController)
	{
		return new URLMappingRestController(urlMappingController);
	}
}
