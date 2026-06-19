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
package nl.clockwork.ebms.server.message.model.core;

import jakarta.xml.bind.JAXBException;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.jaxb.JAXBParser;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class CPA implements Serializable
{
	private static final long serialVersionUID = 1L;
	@NonNull
	String cpaId;
	@NonNull
	String cpaXml;

	public CPA(String cpaId, CollaborationProtocolAgreement cpa) throws JAXBException
	{
		this.cpaId = cpaId;
		this.cpaXml = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa);
	}

	public String getCpa()
	{
		return cpaXml;
	}

	public void setCpa(String cpa)
	{
		this.cpaXml = cpa;
	}
}
