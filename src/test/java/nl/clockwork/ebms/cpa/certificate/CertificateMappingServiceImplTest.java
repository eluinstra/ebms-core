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

import static nl.clockwork.ebms.WithFile.readFileS;
import static nl.clockwork.ebms.jaxb.X509CertificateConverter.parseCertificate;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.security.cert.CertificateException;
import java.util.stream.Stream;
import nl.clockwork.ebms.FixedPostgreSQLContainer;
import nl.clockwork.ebms.PropertiesConfig;
import nl.clockwork.ebms.datasource.DataSourceConfig;
import nl.clockwork.ebms.transaction.TransactionManagerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// @EnabledIf(expression = "#{systemProperties['spring.profiles.active'] == 'test'}")
@TestInstance(Lifecycle.PER_CLASS)
@Testcontainers
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PropertiesConfig.class, CertificateMappingServiceConfig.class, DataSourceConfig.class, TransactionManagerConfig.class})
class CertificateMappingServiceImplTest
{
	@Container
	static final PostgreSQLContainer<?> database = new FixedPostgreSQLContainer();

	@Autowired
	CertificateMappingService mappingService;

	@ParameterizedTest
	@MethodSource("validCertificateMappings")
	void insertValidXML(CertificateMapping mapping)
	{
		assertThatCode(() -> mappingService.setCertificateMapping(mapping)).doesNotThrowAnyException();
	}

	static Stream<Arguments> validCertificateMappings() throws CertificateException
	{
		return Stream.of(
				arguments(
						new CertificateMapping(
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								null)),
				arguments(
						new CertificateMapping(
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								"")),
				arguments(
						new CertificateMapping(
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								"test")));
	}

	@Test
	void getURLMappings() throws CertificateException
	{
		validCertificateMappings().forEach(arg -> mappingService.setCertificateMapping((CertificateMapping)arg.get()[0]));
		assertThat(mappingService.getCertificateMappings()).hasSize(3)
				.contains(
						new CertificateMapping(
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								null))
				.contains(
						new CertificateMapping(
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								""))
				.contains(
						new CertificateMapping(
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))),
								"test"));
	}

	@Test
	void deleteURLMappings() throws CertificateException
	{
		validCertificateMappings().forEach(arg -> mappingService.setCertificateMapping((CertificateMapping)arg.get()[0]));
		assertThat(mappingService.getCertificateMappings()).hasSize(3);
		assertThatCode(
				() -> mappingService.deleteCertificateMapping(parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))), null))
						.doesNotThrowAnyException();
		assertThat(mappingService.getCertificateMappings()).hasSize(2);
		assertThatThrownBy(
				() -> mappingService.deleteCertificateMapping(parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))), null))
						.hasMessageContaining("Certificate not found");
		assertThatCode(() -> mappingService.deleteCertificateMapping(parseCertificate(decodeBase64(readFileS("nl/clockwork/ebms/certificates/localhost.pem"))), ""))
				.doesNotThrowAnyException();
		assertThat(mappingService.getCertificateMappings()).hasSize(1);
	}

	@Test
	void deleteCache()
	{
		assertThatCode(() -> mappingService.deleteCache()).doesNotThrowAnyException();
	}
}
