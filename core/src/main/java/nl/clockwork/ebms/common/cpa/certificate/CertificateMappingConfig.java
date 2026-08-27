/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms.common.cpa.certificate;

import static nl.clockwork.ebms.common.cpa.certificate.CertificateMapping.getCertificateId;

import java.security.cert.X509Certificate;
import java.util.function.BiFunction;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateMappingConfig
{
	@Bean
	public BiFunction<String, X509Certificate, X509Certificate> certificateOverrideLoader(CertificateMappingRepository certificateMappingRepository)
	{
		return (cpaId, certificate) -> certificate != null
				? certificateMappingRepository.getCertificateMapping(getCertificateId(certificate), cpaId, false).orElse(certificate)
				: null;
	}

	@Bean
	public CertificateMappingRepositoryImpl certificateMappingRepository(DataSource dataSource)
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new CertificateMappingRepositoryImpl(jdbcTemplate);
	}
}
