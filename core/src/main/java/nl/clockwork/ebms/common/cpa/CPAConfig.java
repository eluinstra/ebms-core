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
package nl.clockwork.ebms.common.cpa;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.common.security.EbMSKeyStore;
import nl.clockwork.ebms.server.validation.CPAValidator;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CPAConfig
{
	@Bean
	public CPAManager cpaManager(
			CPARepository cpaRepository,
			BiFunction<String, X509Certificate, X509Certificate> overrideCertificate,
			UnaryOperator<String> overrideURL,
			@Qualifier("clientKeyStore") EbMSKeyStore keyStore,
			@Value("${https.useClientCertificate}") boolean useClientCertificate)
	{
		return new CPAManager(cpaRepository, overrideCertificate, overrideURL, keyStore, useClientCertificate);
	}

	@Bean
	public CPAValidator cpaValidator(BiPredicate<String, Instant> isValidCPA)
	{
		return new CPAValidator(isValidCPA);
	}

	@Bean
	public CPARepositoryImpl cpaRepository(DataSource dataSource)
	{
		val jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
		return new CPARepositoryImpl(jdbcTemplate);
	}

	@Bean
	public static BiPredicate<String, Instant> isValidCPA(CPARepository cpaRepository)
	{
		return (cpaId, timestamp) -> cpaRepository.getCPA(cpaId).filter(isValidCPA(timestamp)).isPresent();
	}

	private static Predicate<CollaborationProtocolAgreement> isValidCPA(Instant timestamp)
	{
		return cpa -> StatusValueType.AGREED.equals(cpa.getStatus().getValue())
				&& timestamp.compareTo(cpa.getStart()) >= 0
				&& timestamp.compareTo(cpa.getEnd()) <= 0;
	}
}
