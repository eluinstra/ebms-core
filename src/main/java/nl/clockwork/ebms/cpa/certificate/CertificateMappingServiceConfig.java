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
package nl.clockwork.ebms.cpa.certificate;

import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateMappingServiceConfig
{
	@Bean
	public CertificateMappingService certificateMappingService(CertificateMapper certificateMapper)
	{
		return new CertificateMappingServiceImpl(certificateMapper);
	}

	@Bean
	public CertificateMappingRestService certificateMappingRestService(CertificateMappingService mappingService)
	{
		return new CertificateMappingRestService((CertificateMappingServiceImpl)mappingService);
	}

	@Bean
	public CertificateMapper certificateMapper(DataSource dataSource)
	{
		return new CertificateMapper(certificateMappingDAO(dataSource));
	}

	private CertificateMappingDAO certificateMappingDAO(DataSource dataSource)
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new CertificateMappingDAOImpl(jdbcTemplate);
	}
}
