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
package nl.clockwork.ebms.common.util;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class XSDValidatorTest
{
	@Test
	void loadsCpaSchemaAndValidatesValidCpa() throws Exception
	{
		// Constructing the validator is the key check: the cpp-cpa-2_0.xsd imports xlink.xsd,
		// xmldsig-core-schema.xsd and xml.xsd by relative schemaLocation, which must resolve from the
		// classpath with ACCESS_EXTERNAL_SCHEMA="" (no external access) in effect.
		XSDValidator validator = new XSDValidator("/nl/clockwork/ebms/xsd/cpp-cpa-2_0.xsd");
		String cpa = IOUtils.toString(XSDValidatorTest.class.getResourceAsStream("/nl/clockwork/ebms/cpas/cpaStubEBF.rm.https.signed.xml"), StandardCharsets.UTF_8);
		assertThatCode(() -> validator.validate(cpa)).doesNotThrowAnyException();
	}

	@Test
	void loadsMessageHeaderSchema()
	{
		// The message header schema also imports sibling schemas (envelope.xsd, etc.); construction must
		// succeed without external schema access.
		assertThatCode(() -> new XSDValidator("/nl/clockwork/ebms/xsd/msg-header-2_0.xsd")).doesNotThrowAnyException();
	}
}
