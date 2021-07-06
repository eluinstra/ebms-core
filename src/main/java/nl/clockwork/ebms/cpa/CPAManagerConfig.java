/**
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
package nl.clockwork.ebms.cpa;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CPAManagerConfig
{
	@Autowired
	DataSource dataSource;

	@Bean
	public CPAManager cpaManager()
	{
		return new CPAManager(cpaDAO(),urlMapper());
	}

	@Bean
	public CPADAO cpaDAO()
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new CPADAOImpl(jdbcTemplate);
	}

	@Bean
	public URLMapper urlMapper()
	{
		return new URLMapper(urlMappingDAO());
	}

	@Bean
	public URLMappingDAO urlMappingDAO()
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new URLMappingDAOImpl(jdbcTemplate);
	}

	@Bean
	public CertificateMapper certificateMapper()
	{
		return new CertificateMapper(certificateMappingDAO());
	}

	@Bean
	public CertificateMappingDAO certificateMappingDAO()
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new CertificateMappingDAOImpl(jdbcTemplate);
	}
}
