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
package nl.clockwork.ebms.util;

import java.util.ArrayList;
import java.util.List;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.ToPartyInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Constituent;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Packaging;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Packaging.CompositeList.Composite;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Packaging.CompositeList.Encapsulation;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SimplePart;

public class EbMSMessageContentValidator
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private boolean validateDataSources;

	public EbMSMessageContentValidator(EbMSDAO ebMSDAO, boolean validateDataSources)
	{
		this.ebMSDAO = ebMSDAO;
		this.validateDataSources = validateDataSources;
	}
	
	public void validate(EbMSMessageContent content)
	{
		try
		{
			EbMSMessageContext context = content.getContext();
			validateContext(context);
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(context.getCpaId());
			if (cpa == null)
				throw new EbMSMessageContextValidationException("No CPA found for: context.cpaId=" + context.getCpaId());
			FromPartyInfo fromPartyInfo = validateFromParty(cpa,context);
			validateToParty(cpa,context,fromPartyInfo);
			if (validateDataSources)
				validateDataSources(cpa,content,fromPartyInfo);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageContextValidatorException(e);
		}
	}

	private void validateToParty(CollaborationProtocolAgreement cpa, EbMSMessageContext context, FromPartyInfo fromPartyInfo)
	{
		//ToPartyInfo toPartyInfo = CPAUtils.getToPartyInfo(cpa,(ActionBindingType)fromPartyInfo.getCanSend().getOtherPartyActionBinding());
		ToPartyInfo toPartyInfo1 = CPAUtils.getToPartyInfo(cpa,context.getToRole(),context.getService(),context.getAction());
		//if (toPartyInfo == null && toPartyInfo1 == null)
		if (fromPartyInfo.getCanSend().getOtherPartyActionBinding() == null && toPartyInfo1 == null)
		{
			StringBuffer msg = new StringBuffer();
			msg.append("No CanReceive action found for:");
			msg.append(" context.cpaId=").append(context.getCpaId());
			if (fromPartyInfo.getCanSend().getOtherPartyActionBinding() != null && context.getFromRole() != null)
				msg.append(", context.fromRole=").append(context.getFromRole());
			if (context.getToRole() != null)
				msg.append(", context.toRole=").append(context.getToRole());
			msg.append(", context.service=").append(context.getService());
			msg.append(", context.action=").append(context.getAction());
			throw new EbMSMessageContextValidationException(msg.toString());
		}
		//else if (toPartyInfo != null && toPartyInfo1 != null && toPartyInfo.getCanReceive().getThisPartyActionBinding() != toPartyInfo1.getCanReceive().getThisPartyActionBinding())
		else if (fromPartyInfo.getCanSend().getOtherPartyActionBinding() != null && toPartyInfo1 != null && fromPartyInfo.getCanSend().getOtherPartyActionBinding() != toPartyInfo1.getCanReceive().getThisPartyActionBinding())
			throw new EbMSMessageContextValidationException("to party does not match from party for this action. Leave context.toRole empty!");
	}

	private void validateContext(EbMSMessageContext context)
	{
		if (StringUtils.isEmpty(context.getCpaId()))
			throw new EbMSMessageContextValidationException("context.cpaId cannot be empty!");
		if (StringUtils.isEmpty(context.getService()))
			throw new EbMSMessageContextValidationException("context.service cannot be empty!");
		if (StringUtils.isEmpty(context.getAction()))
			throw new EbMSMessageContextValidationException("context.action cannot be empty!");
	}
	
	private FromPartyInfo validateFromParty(CollaborationProtocolAgreement cpa, EbMSMessageContext context)
	{
		FromPartyInfo fromPartyInfo = CPAUtils.getFromPartyInfo(cpa,context.getFromRole(),context.getService(),context.getAction());
		if (fromPartyInfo == null)
		{
			StringBuffer msg = new StringBuffer();
			msg.append("No CanSend action found for:");
			msg.append(" context.cpaId=").append(context.getCpaId());
			if (context.getFromRole() != null)
				msg.append(", context.fromRole=").append(context.getFromRole());
			msg.append(", context.service=").append(context.getService());
			msg.append(", context.action=").append(context.getAction());
			throw new EbMSMessageContextValidationException(msg.toString());
		}
		return fromPartyInfo;
	}

	private void validateDataSources(CollaborationProtocolAgreement cpa, EbMSMessageContent content, FromPartyInfo partyInfo)
	{
		Packaging packaging = CPAUtils.getPackaging(partyInfo.getCanSend());
		if (packaging.getProcessingCapabilities().isParse() && packaging.getCompositeList().get(0).getEncapsulationOrComposite().size() > 0)
		{
			List<EbMSDataSource> dataSources = new ArrayList<EbMSDataSource>(content.getDataSources());
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
							EbMSDataSource attachment = findDataSource(dataSources,simplePart);
							if (attachment != null)
								dataSources.remove(attachment);
							else
								throw new EbMSMessageContextValidationException("SimplePart " + simplePart.getId() + " not found!");
						}
						if (idref instanceof Encapsulation)
						{
							Encapsulation encapsulation = (Encapsulation)idref;
							EbMSDataSource attachment = findDataSource(dataSources,encapsulation);
							if (attachment != null)
								dataSources.remove(attachment);
							else
								throw new EbMSMessageContextValidationException("Packaging Encapsulation " + encapsulation.getId() + " not found!");
						}
						if (idref instanceof Composite)
						{
							Composite composite = (Composite)idref;
							EbMSDataSource attachment = findDataSource(dataSources,composite);
							if (attachment != null)
								dataSources.remove(attachment);
							else
								throw new EbMSMessageContextValidationException("Packaging Composite " + composite.getId() + " not found!");
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

	private EbMSDataSource findDataSource(List<EbMSDataSource> attachments, SimplePart simplePart)
	{
		for (EbMSDataSource attachment : attachments)
			if (attachment.getContentType().equals(simplePart.getMimetype()))
				return attachment;
		return null;
	}

	private EbMSDataSource findDataSource(List<EbMSDataSource> attachments, Encapsulation encapsulation)
	{
		for (EbMSDataSource attachment : attachments)
			if (attachment.getContentType().equals(encapsulation.getMimetype()))
				return attachment;
		return null;
	}

	private EbMSDataSource findDataSource(List<EbMSDataSource> attachments, Composite composite)
	{
		for (EbMSDataSource attachment : attachments)
			if (attachment.getContentType().equals(composite.getMimetype()))
				return attachment;
		return null;
	}

}
