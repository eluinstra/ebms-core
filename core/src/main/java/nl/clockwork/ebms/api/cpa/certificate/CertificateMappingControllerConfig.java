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
package nl.clockwork.ebms.api.cpa.certificate;

import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.common.cpa.certificate.CertificateMappingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateMappingControllerConfig
{
	@Bean
	public CertificateMappingController certificateMappingController(CertificateMappingRepository certificateMappingRepository)
	{
		return new CertificateMappingControllerImpl(certificateMappingRepository);
	}

	@Bean
	public CertificateMappingRestController certificateMappingRestController(CertificateMappingRepository certificateMappingRepository)
	{
		return new CertificateMappingRestController(new CertificateMappingControllerImpl(certificateMappingRepository));
	}

	@Bean
	public CertificateMappingRepository certificateMappingRepository(DataSource dataSource)
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		val result = new CertificateMappingRepository(jdbcTemplate);
		result.setSelf(result);
		return result;
	}
}
