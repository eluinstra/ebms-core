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
package nl.clockwork.ebms.cpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import nl.clockwork.ebms.FixedPostgreSQLContainer;
import nl.clockwork.ebms.PropertiesConfig;
import nl.clockwork.ebms.WithFile;
import nl.clockwork.ebms.cpa.url.URLMappingServiceConfig;
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
@ContextConfiguration(classes = {PropertiesConfig.class,CPAServiceConfig.class,CPAManagerConfig.class,URLMappingServiceConfig.class,DataSourceConfig.class,
		TransactionManagerConfig.class})
class CPAServiceImplTest implements WithFile
{
	@Container
	static final PostgreSQLContainer<?> database = new FixedPostgreSQLContainer();

	@Autowired
	CPAService cpaService;

	@ParameterizedTest
	@MethodSource("invalidCPAs")
	void validateInvalidXML(String cpa, String message)
	{
		assertThatThrownBy(() -> cpaService.validateCPA(cpa)).hasMessageContaining(message);
	}

	private static Stream<Arguments> invalidCPAs()
	{
		return Stream.of(arguments(null,"java.lang.NullPointerException"),
				arguments("","Premature end of file."),
				arguments("<","XML document structures must start and end within the same entity."),
				arguments("<xml","XML document structures must start and end within the same entity."),
				arguments("x<xml/>","Content is not allowed in prolog."),
				arguments("<>","The markup in the document preceding the root element must be well-formed."),
				arguments("<<xml/>","The markup in the document preceding the root element must be well-formed."),
				arguments("<xml/>","Cannot find the declaration of element 'xml'"),
				arguments(" <xml/> ","Cannot find the declaration of element 'xml'"));
	}

	@ParameterizedTest
	@MethodSource("validCPAs")
	void validateValidXML(String path)
	{
		assertThatCode(() -> cpaService.validateCPA(readFile(path))).doesNotThrowAnyException();
	}

	private static Stream<Arguments> validCPAs()
	{
		return Stream.of(arguments("nl/clockwork/ebms/cpas/cpa-cv-http.xml"),
				arguments("nl/clockwork/ebms/cpas/cpa-cv-https.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.be.http.signed.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.be.http.unsigned.sync.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.be.http.unsigned.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.be.https.signed.sync.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.be.https.signed.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.http.signed-sha256.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.http.signed.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.http.unsigned.sync.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.http.unsigned.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.https.signed-sha256.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.https.signed.ackSigned.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.https.signed.encrypted.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.https.signed.sync.xml"),
				arguments("nl/clockwork/ebms/cpas/cpaStubEBF.rm.https.signed.xml"));
	}

	@ParameterizedTest
	@MethodSource("validCPAs")
	void insertValidXML(String path)
	{
		assertThatCode(() -> cpaService.insertCPA(readFile(path),true)).doesNotThrowAnyException();
	}

	@Test
	void getCPAIds()
	{
		validCPAs().forEach(path -> cpaService.insertCPA(readFile((String)path.get()[0]),true));
		assertThat(cpaService.getCPAIds()).hasSize(15)
				.contains("CPAID_EchoService-1-0")
				.contains("cpaStubEBF.be.http.signed")
				.contains("cpaStubEBF.be.http.unsigned.sync")
				.contains("cpaStubEBF.be.http.unsigned")
				.contains("cpaStubEBF.be.https.signed.sync")
				.contains("cpaStubEBF.be.https.signed")
				.contains("cpaStubEBF.rm.http.signed-sha256")
				.contains("cpaStubEBF.rm.http.signed")
				.contains("cpaStubEBF.rm.http.unsigned.sync")
				.contains("cpaStubEBF.rm.http.unsigned")
				.contains("cpaStubEBF.rm.https.signed-sha256")
				.contains("cpaStubEBF.rm.https.signed.ackSigned")
				.contains("cpaStubEBF.rm.https.signed.encrypted")
				.contains("cpaStubEBF.rm.https.signed.sync")
				.contains("cpaStubEBF.rm.https.signed");
	}

	@Test
	void getCPA()
	{
		validCPAs().forEach(path -> cpaService.insertCPA(readFile((String)path.get()[0]),true));
		assertThat(cpaService.getCPAIds()).hasSizeGreaterThan(0);
		assertThat(cpaService.getCPA("cpaStubEBF.rm.https.signed")).contains("cpaid=\"cpaStubEBF.rm.https.signed\"");
	}

	@Test
	void deleteCPA()
	{
		validCPAs().forEach(path -> cpaService.insertCPA(readFile((String)path.get()[0]),true));
		assertThat(cpaService.getCPAIds()).hasSize(15);
		assertThatCode(() -> cpaService.deleteCPA("CPAID_EchoService-1-0")).doesNotThrowAnyException();
		assertThat(cpaService.getCPAIds()).hasSize(14);
		assertThatThrownBy(() -> cpaService.deleteCPA("CPAID_EchoService-1-0")).hasMessageContaining("CPA not found");
		assertThatCode(() -> cpaService.deleteCPA("cpaStubEBF.be.http.signed")).doesNotThrowAnyException();
		assertThat(cpaService.getCPAIds()).hasSize(13);
	}

	@Test
	void deleteCache()
	{
		assertThatCode(() -> cpaService.deleteCache()).doesNotThrowAnyException();
	}
}
