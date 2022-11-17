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

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class URLMappingServiceConfig
{
	@Bean
	public URLMappingService urlMappingService(URLMapper urlMapper)
	{
		return new URLMappingServiceImpl(urlMapper);
	}

	@Bean
	public URLMapper urlMapper(DataSource dataSource)
	{
		return new URLMapper(urlMappingDAO(dataSource));
	}

	private URLMappingDAO urlMappingDAO(DataSource dataSource)
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new URLMappingDAOImpl(jdbcTemplate);
	}
}
