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
package nl.clockwork.ebms.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.MessageStatus;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.signature.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageContextValidator;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.xml.sax.SAXException;

public class EbMSMessageServiceImpl implements EbMSMessageService
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private DeliveryManager deliveryManager;
	private EbMSDAO ebMSDAO;
	private EbMSSignatureGenerator signatureGenerator;

	@Override
	public void ping(String cpaId, Party fromParty, Party toParty) throws EbMSMessageServiceException
	{
		try
		{
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(cpaId);
			EbMSMessage request = EbMSMessageUtils.createEbMSPing(cpa,fromParty,toParty);
			EbMSMessage response = deliveryManager.sendMessage(cpa,request);
			if (response != null)
			{
				if (!EbMSAction.PONG.action().equals(response.getMessageHeader().getAction()))
					throw new EbMSMessageServiceException("No valid response received!");
			}
			else
				throw new EbMSMessageServiceException("No response received!");
		}
		catch (DatatypeConfigurationException | JAXBException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}
	
	@Override
	public String sendMessage(EbMSMessageContent messageContent) throws EbMSMessageServiceException
	{
		try
		{
			new EbMSMessageContextValidator(ebMSDAO).validate(messageContent.getContext());
			final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageContent.getContext().getCpaId());
			//TODO ebMSMessageContentToEbMSDocument
			final EbMSMessage message = EbMSMessageUtils.ebMSMessageContentToEbMSMessage(cpa,messageContent);
			message.setMessage(EbMSMessageUtils.createSOAPMessage(message));
			signatureGenerator.generate(cpa,message);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.insertMessage(new Date(),message,EbMSMessageStatus.SENT);
						List<EbMSEvent> events = EbMSMessageUtils.createEbMSSendEvents(ebMSDAO.getCPA(message.getMessageHeader().getCPAId()),message,CPAUtils.getUri(cpa,message));
						ebMSDAO.insertEvents(events);
					}
				}
			);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (DAOException | DatatypeConfigurationException | SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerFactoryConfigurationError | TransformerException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			if (maxNr == null)
				return ebMSDAO.getMessageIds(messageContext,EbMSMessageStatus.RECEIVED);
			else
				return ebMSDAO.getMessageIds(messageContext,EbMSMessageStatus.RECEIVED,maxNr);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public EbMSMessageContent getMessage(String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			EbMSMessageContent result = ebMSDAO.getMessageContent(messageId);
			if (process != null && process)
				ebMSDAO.updateMessage(messageId,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
			return result;
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessage(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.updateMessage(messageId,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessages(List<String> messageIds) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.updateMessages(messageIds,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}
	
	@Override
	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			EbMSMessageContext context = ebMSDAO.getMessageContext(messageId);
			if (context == null)
				throw new EbMSMessageServiceException("No message found with messageId " + messageId + "!");
			else if (Constants.EBMS_SERVICE_URI.equals(context.getService()))
				throw new EbMSMessageServiceException("Message with messageId " + messageId + " is an EbMS service message!");
			else
			{
				CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(context.getCpaId());
				EbMSMessage request = EbMSMessageUtils.createEbMSStatusRequest(cpa,CPAUtils.getFromParty(cpa,context.getFromRole(),context.getService(),context.getAction()),CPAUtils.getToParty(cpa,context.getToRole(),context.getService(),context.getAction()),messageId);
				EbMSMessage response = deliveryManager.sendMessage(cpa,request);
				if (response != null)
				{
					if (EbMSAction.STATUS_RESPONSE.action().equals(response.getMessageHeader().getAction()) && response.getStatusResponse() != null)
						return new MessageStatus(response.getStatusResponse().getTimestamp() == null ? null : response.getStatusResponse().getTimestamp().toGregorianCalendar().getTime(),EbMSMessageStatus.get(response.getStatusResponse().getMessageStatus()));
					else
						throw new EbMSMessageServiceException("No valid response received!");
				}
				else
					throw new EbMSMessageServiceException("No response received!");
			}
		}
		catch (DAOException | DatatypeConfigurationException | JAXBException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public MessageStatus getMessageStatus(String cpaId, Party fromParty, Party toParty, String messageId) throws EbMSMessageServiceException
	{
		try
		{
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(cpaId);
			EbMSMessage request = EbMSMessageUtils.createEbMSStatusRequest(cpa,fromParty,toParty,messageId);
			EbMSMessage response = deliveryManager.sendMessage(cpa,request);
			if (response != null)
			{
				if (EbMSAction.STATUS_RESPONSE.action().equals(response.getMessageHeader().getAction()) && response.getStatusResponse() != null)
					return new MessageStatus(response.getStatusResponse().getTimestamp() == null ? null : response.getStatusResponse().getTimestamp().toGregorianCalendar().getTime(),EbMSMessageStatus.get(response.getStatusResponse().getMessageStatus()));
				else
					throw new EbMSMessageServiceException("No valid response received!");
			}
			else
				throw new EbMSMessageServiceException("No response received!");
		}
		catch (DAOException | DatatypeConfigurationException | JAXBException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	public void setDeliveryManager(DeliveryManager deliveryManager)
	{
		this.deliveryManager = deliveryManager;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setSignatureGenerator(EbMSSignatureGenerator signatureGenerator)
	{
		this.signatureGenerator = signatureGenerator;
	}
	
}
