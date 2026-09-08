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
package nl.clockwork.ebms.api.cpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import nl.clockwork.ebms.PropertiesConfig;
import nl.clockwork.ebms.WithFile;
import nl.clockwork.ebms.api.cpa.certificate.CertificateMappingControllerConfig;
import nl.clockwork.ebms.api.cpa.url.URLMappingControllerConfig;
import nl.clockwork.ebms.common.cpa.CPAConfig;
import nl.clockwork.ebms.common.cpa.certificate.CertificateMappingConfig;
import nl.clockwork.ebms.common.cpa.url.URLMappingConfig;
import nl.clockwork.ebms.datasource.DataSourceConfig;
import nl.clockwork.ebms.security.KeyStoreConfig;
import nl.clockwork.ebms.transaction.TransactionManagerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Regression guard for the <b>REST</b> (JAX-RS) CPA entry points against billion-laughs
 * and XXE.
 *
 * <p>{@code CPARestController} takes the CPA as a {@code text/plain} {@link String} and never
 * XML-parses it at the transport layer; the first parse happens inside
 * {@code CPAControllerImpl} (XSD validation, then JAXB). Those parsers are hardened
 * (disallow-doctype-decl / ACCESS_EXTERNAL_DTD=""), so a DOCTYPE-based attack must be rejected
 * with a 400 Bad Request — not expand (DoS) and not resolve external entities (XXE).
 */
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		classes = {PropertiesConfig.class, KeyStoreConfig.class, CPAConfig.class, CPAControllerConfig.class, CertificateMappingConfig.class,
				CertificateMappingControllerConfig.class, URLMappingConfig.class, URLMappingControllerConfig.class, DataSourceConfig.class,
				TransactionManagerConfig.class})
class CPARestControllerSecurityIT implements WithFile
{
	@Autowired
	CPARestController cpaRestController;

	/** Escalating internal-entity billion-laughs bomb rooted at the CPA element. */
	private static final String BILLION_LAUGHS = "<!DOCTYPE CollaborationProtocolAgreement [\n"
			+ " <!ENTITY a \"xxxxxxxxxx\">\n"
			+ " <!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\">\n"
			+ " <!ENTITY c \"&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;\">\n"
			+ " <!ENTITY d \"&c;&c;&c;&c;&c;&c;&c;&c;&c;&c;\">\n"
			+ " <!ENTITY e \"&d;&d;&d;&d;&d;&d;&d;&d;&d;&d;\">\n"
			+ " <!ENTITY f \"&e;&e;&e;&e;&e;&e;&e;&e;&e;&e;\">\n"
			+ "]>\n<CollaborationProtocolAgreement>&f;</CollaborationProtocolAgreement>";

	/** XXE payload: an external SYSTEM entity that must never be resolved. */
	private static final String XXE = "<!DOCTYPE CollaborationProtocolAgreement [<!ENTITY xxe SYSTEM \"file:///tmp/ebms-xxe-canary\">]>\n"
			+ "<CollaborationProtocolAgreement>&xxe;</CollaborationProtocolAgreement>";

	@Test
	void validateEndpointRejectsBillionLaughs()
	{
		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> cpaRestController.validateCPA(BILLION_LAUGHS));
		assertThat(ex.getResponse().getStatus()).as("billion-laughs must be rejected, not expanded").isEqualTo(400);
	}

	@Test
	void validateEndpointRejectsXxe()
	{
		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> cpaRestController.validateCPA(XXE));
		assertThat(ex.getResponse().getStatus()).as("XXE doctype must be rejected, not resolved").isEqualTo(400);
	}

	@Test
	void insertEndpointRejectsBillionLaughs()
	{
		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> cpaRestController.insertCPA(BILLION_LAUGHS, false));
		assertThat(ex.getResponse().getStatus()).as("billion-laughs must be rejected before any insert").isEqualTo(400);
	}

	@Test
	void insertEndpointRejectsXxe()
	{
		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> cpaRestController.insertCPA(XXE, false));
		assertThat(ex.getResponse().getStatus()).as("XXE doctype must be rejected before any insert").isEqualTo(400);
	}

	@Test
	void validateEndpointStillAcceptsValidCpa()
	{
		// Positive control: the endpoint is not blanket-400; a well-formed, schema-valid CPA passes.
		assertThatCode(() -> cpaRestController.validateCPA(readFile("nl/clockwork/ebms/cpas/cpa-cv-http.xml"))).doesNotThrowAnyException();
	}

	@Test
	void insertEndpointStillAcceptsValidCpa()
	{
		// Positive control: a valid CPA is parsed and stored (overwrite keeps this idempotent).
		String cpaId = cpaRestController.insertCPA(readFile("nl/clockwork/ebms/cpas/cpa-cv-http.xml"), true);
		assertThat(cpaId).as("insertCPA returns the stored CPA id").isNotBlank();
	}
}
