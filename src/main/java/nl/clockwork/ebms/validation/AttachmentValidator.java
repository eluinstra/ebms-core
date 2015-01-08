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
package nl.clockwork.ebms.validation;

import java.util.ArrayList;
import java.util.List;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Constituent;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Packaging;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Packaging.CompositeList.Composite;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Packaging.CompositeList.Encapsulation;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SimplePart;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

public class AttachmentValidator
{
	protected transient Log logger = LogFactory.getLog(getClass());

	public void validate(CollaborationProtocolAgreement cpa, EbMSMessage message) throws EbMSValidationException
	{
		MessageHeader messageHeader = message.getMessageHeader();
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
		CanSend canSend = CPAUtils.getCanSend(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
		Packaging packaging = CPAUtils.getPackaging(canSend);
		if (packaging.getProcessingCapabilities().isParse() && packaging.getCompositeList().get(0).getEncapsulationOrComposite().size() > 0)
		{
			List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>(message.getAttachments());
			Object encapsulationOrComposite = packaging.getCompositeList().get(0).getEncapsulationOrComposite().get(packaging.getCompositeList().get(0).getEncapsulationOrComposite().size() - 1);
			if (encapsulationOrComposite instanceof Composite)
			{
				Composite root = (Composite)encapsulationOrComposite;
				if (root.getMimetype().endsWith("text/xml"))
				{
					for (Constituent constituent : root.getConstituent())
					{
						Object idref = constituent.getIdref();
						if (idref instanceof SimplePart)
						{
							SimplePart simplePart = (SimplePart)idref;
							EbMSAttachment attachment = findAttachment(attachments,simplePart);
							if (attachment != null)
								attachments.remove(attachment);
							else
								throw new EbMSValidationException(EbMSMessageUtils.createError("//Body/Manifest/Reference",Constants.EbMSErrorCode.MIME_PROBLEM.errorCode(),"SimplePart " + simplePart.getId() + " not found!"));
						}
						if (idref instanceof Encapsulation)
						{
							Encapsulation encapsulation = (Encapsulation)idref;
							EbMSAttachment attachment = findAttachment(attachments,encapsulation);
							if (attachment != null)
								attachments.remove(attachment);
							else
								throw new EbMSValidationException(EbMSMessageUtils.createError("//Body/Manifest/Reference",Constants.EbMSErrorCode.MIME_PROBLEM.errorCode(),"Packaging Encapsulation " + encapsulation.getId() + " not found!"));
						}
						if (idref instanceof Composite)
						{
							Composite composite = (Composite)idref;
							EbMSAttachment attachment = findAttachment(attachments,composite);
							if (attachment != null)
								attachments.remove(attachment);
							else
								throw new EbMSValidationException(EbMSMessageUtils.createError("//Body/Manifest/Reference",Constants.EbMSErrorCode.MIME_PROBLEM.errorCode(),"Packaging Composite " + composite.getId() + " not found!"));
						}
					}
					// if (attachments.size() > 0) ???
				}
				else if (root.getMimetype().endsWith("multipart/related") /*&& "type=&quot;text/xml&quot; version=&quot;1.0&quot".equals(composite.getMimeparameters())*/)
				{
					logger.warn("Packaging validation not executed! multipart/related not supported!");
				}
			}
			else
				logger.warn("Packaging validation not executed! Cannot find top-level definition.");
		}
	}

	private EbMSAttachment findAttachment(List<EbMSAttachment> attachments, SimplePart simplePart)
	{
		for (EbMSAttachment attachment : attachments)
			if (attachment.getContentType().equals(simplePart.getMimetype()))
				return attachment;
		return null;
	}

	private EbMSAttachment findAttachment(List<EbMSAttachment> attachments, Encapsulation encapsulation)
	{
		for (EbMSAttachment attachment : attachments)
			if (attachment.getContentType().equals(encapsulation.getMimetype()))
				return attachment;
		return null;
	}

	private EbMSAttachment findAttachment(List<EbMSAttachment> attachments, Composite composite)
	{
		for (EbMSAttachment attachment : attachments)
			if (attachment.getContentType().equals(composite.getMimetype()))
				return attachment;
		return null;
	}

}
