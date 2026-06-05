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

import java.util.function.UnaryOperator;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.delivery.task.URLMappingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class URLMappingConfig
{
	@Bean
	public UnaryOperator<String> overrideURL(URLMappingRepository urlMappingRepository)
	{
		return urlMappingRepository::getURL;
	}

	@Bean
	public URLMappingRepositoryImpl urlMappingRepository(@org.springframework.lang.NonNull DataSource dataSource)
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new URLMappingRepositoryImpl(jdbcTemplate);
	}
}
