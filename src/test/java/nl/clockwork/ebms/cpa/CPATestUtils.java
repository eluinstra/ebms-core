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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import io.vavr.Function1;
import lombok.val;
import nl.clockwork.ebms.jaxb.JAXBParser;

public class CPATestUtils
{
	public static Function1<String, Optional<CollaborationProtocolAgreement>> cpaCache = Function1.of(CPATestUtils::loadCPA).memoized();

  public static Optional<CollaborationProtocolAgreement> loadCPA(String cpaId)
	{
		try
		{
			val s = IOUtils.toString(CPAUtils.class.getResourceAsStream("/nl/clockwork/ebms/cpa/" + cpaId + ".xml"),Charset.forName("UTF-8"));
			return Optional.of(JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(s));
		}
		catch (IOException | JAXBException e)
		{
			return Optional.empty();
		}
	}
}
