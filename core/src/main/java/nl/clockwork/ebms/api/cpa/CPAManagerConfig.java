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
package nl.clockwork.ebms.api.cpa;

import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.api.cpa.certificate.CertificateMappingRepository;
import nl.clockwork.ebms.api.cpa.url.URLMapper;
import nl.clockwork.ebms.security.EbMSKeyStore;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CPAManagerConfig
{
	@Bean
	public CPAManager cpaManager(CPARepository cpaRepository)
	{
		return new CPAManager(cpaRepository);
	}

	@Bean
	public CPAQueryManager cpaQueryManager(
			CPAManager cpaManager,
			CertificateMappingRepository certificateMappingRepository,
			URLMapper urlMapper,
			@Qualifier("clientKeyStore") EbMSKeyStore keyStore,
			@Value("${https.useClientCertificate}") boolean useClientCertificate)
	{
		return new CPAQueryManager(cpaManager, certificateMappingRepository, urlMapper, keyStore, useClientCertificate);
	}

	@Bean
	public CPARepository cpaRepository(DataSource dataSource)
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new CPARepository(jdbcTemplate);
	}
}
