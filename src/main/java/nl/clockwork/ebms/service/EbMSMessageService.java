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

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.MessageStatus;
import nl.clockwork.ebms.model.Party;

@WebService(targetNamespace="http://www.ordina.nl/ebms/2.2")
public interface EbMSMessageService
{
	@WebMethod(operationName="Ping")
	void ping(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId, @WebParam(name="FromParty") @XmlElement(required=true) Party fromParty, @WebParam(name="ToParty") @XmlElement(required=true) Party toParty) throws EbMSMessageServiceException;
	
	@WebResult(name="MessageId")
	@WebMethod(operationName="SendMessage")
	String sendMessage(@WebParam(name="Message") @XmlElement(required=true) EbMSMessageContent messageContent) throws EbMSMessageServiceException;

	@WebResult(name="MessageIds")
	@WebMethod(operationName="GetMessageIds")
	//@ResponseWrapper(className="nl.clockwork.ebms.service.GetMessageIdsResponse")
	List<String> getMessageIds(@WebParam(name="MessageContext") @XmlElement(required=true) EbMSMessageContext messageContext, @WebParam(name="MaxNr") Integer maxNr) throws EbMSMessageServiceException;

	@WebResult(name="Message")
	@WebMethod(operationName="GetMessage")
	EbMSMessageContent getMessage(@WebParam(name="MessageId") @XmlElement(required=true) String messageId, @WebParam(name="Process") Boolean process) throws EbMSMessageServiceException;

	@WebMethod(operationName="ProcessMessage")
	void processMessage(@WebParam(name="MessageId") @XmlElement(required=true) String messageId) throws EbMSMessageServiceException;

	@WebMethod(operationName="ProcessMessages")
	void processMessages(@WebParam(name="MessageId") @XmlElement(required=true) List<String> messageIds) throws EbMSMessageServiceException;

	@WebResult(name="MessageStatus")
	@WebMethod(operationName="GetMessageStatusByMessageId")
	MessageStatus getMessageStatus(@WebParam(name="MessageId") @XmlElement(required=true) String messageId) throws EbMSMessageServiceException;

	@WebResult(name="MessageStatus")
	@WebMethod(operationName="GetMessageStatus")
	MessageStatus getMessageStatus(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId, @WebParam(name="FromParty") @XmlElement(required=true) Party fromParty, @WebParam(name="ToParty") @XmlElement(required=true) Party toParty, @WebParam(name="MessageId") @XmlElement(required=true) String messageId) throws EbMSMessageServiceException;

}
