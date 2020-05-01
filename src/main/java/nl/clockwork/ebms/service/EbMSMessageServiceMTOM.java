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
import javax.xml.ws.soap.MTOM;

import nl.clockwork.ebms.event.listener.EbMSMessageEventType;
import nl.clockwork.ebms.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSMessageEvent;
import nl.clockwork.ebms.model.MessageStatus;
import nl.clockwork.ebms.model.Party;

@MTOM(enabled=true)
@WebService(name = "EbMSMessageService", targetNamespace="http://www.ordina.nl/ebms/2.17", serviceName = "EbMSMessageServiceImplService", endpointInterface = "EbMSMessageServiceImplServiceSoapBinding", portName = "EbMSMessageServiceImplPort")
public interface EbMSMessageServiceMTOM
{
	/**
	 * Performs an EbMS ping action for CPA cpaId, from party fromParty and to party toParty
	 * 
	 * @param cpaId
	 * @param fromParty
	 * @param toParty
	 * @throws EbMSMessageServiceException
	 */
	@WebMethod(operationName="Ping")
	void ping(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId, @WebParam(name="FromParty") @XmlElement(required=true) Party fromParty, @WebParam(name="ToParty") @XmlElement(required=true) Party toParty) throws EbMSMessageServiceException;

	/**
	 * Sends the message content messageContent as an EbMS message using MTOM/XOP
	 * 
	 * @param messageContent
	 * @return The messageId of the generated EbMS message
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name="MessageId")
	@WebMethod(operationName="SendMessage")
	String sendMessageMTOM(@WebParam(name="Message") @XmlElement(required=true) EbMSMessageContentMTOM messageContent) throws EbMSMessageServiceException;

	/**
	 * Resends the content of message identified by messageId as an EbMS message
	 * 
	 * @param messageId
	 * @return The messageId of the generated EbMS message
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name="MessageId")
	@WebMethod(operationName="ResendMessage")
	String resendMessage(@WebParam(name="MessageId") @XmlElement(required=true) String messageId) throws EbMSMessageServiceException;

	/**
	 * Gets all messageIds of messages with the RECEIVED status that satisfy the filter messageContext. If maxNr is given, then maxNr messageIds are returned
	 * 
	 * @param messageContext
	 * @param maxNr
	 * @return The list of messageIds
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name="MessageIds")
	@WebMethod(operationName="GetMessageIds")
	List<String> getMessageIds(@WebParam(name="MessageContext") @XmlElement(required=true) EbMSMessageContext messageContext, @WebParam(name="MaxNr") Integer maxNr) throws EbMSMessageServiceException;

	/**
	 * Gets the message content of the message identified by messageId using MTOM/XOP. If process is true, the message is given the status PROCESSED, which means that it is no longer returned in the list of getMessageIds
	 * 
	 * @param messageId
	 * @param process
	 * @return The messageContent
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name="Message")
	@WebMethod(operationName="GetMessage")
	EbMSMessageContentMTOM getMessageMTOM(@WebParam(name="MessageId") @XmlElement(required=true) String messageId, @WebParam(name="Process") Boolean process) throws EbMSMessageServiceException;

	/**
	 * Sets the status of the message identified by messageId to PROCESSED, so that it is no longer returned in the list of getMessageIds
	 * 
	 * @param messageId
	 * @throws EbMSMessageServiceException
	 */
	@WebMethod(operationName="ProcessMessage")
	void processMessage(@WebParam(name="MessageId") @XmlElement(required=true) String messageId) throws EbMSMessageServiceException;

	/**
	 * Gets the message status of the message identified by messageId
	 * 
	 * @param messageId
	 * @return The message status
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name="MessageStatus")
	@WebMethod(operationName="GetMessageStatusByMessageId")
	MessageStatus getMessageStatus(@WebParam(name="MessageId") @XmlElement(required=true) String messageId) throws EbMSMessageServiceException;

	/**
	 * Gets the message status of the message identified by messageId for CPA cpaId, from party fromParty and to party toParty
	 * 
	 * @param cpaId
	 * @param fromParty
	 * @param toParty
	 * @param messageId
	 * @return The message status
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name="MessageStatus")
	@WebMethod(operationName="GetMessageStatus")
	MessageStatus getMessageStatus(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId, @WebParam(name="FromParty") @XmlElement(required=true) Party fromParty, @WebParam(name="ToParty") @XmlElement(required=true) Party toParty, @WebParam(name="MessageId") @XmlElement(required=true) String messageId) throws EbMSMessageServiceException;

	/**
	 * Gets the events that satisfy the messageContext filter and the eventTypes eventTypes. If maxNr is included, then maxNr events are returned. The possible event types are:
	 * - RECEIVED – when a message is received
	 * - DELIVERED – if a message has been sent successfully
	 * - FAILED – if a message returns an error while sending
	 * - EXPIRED – if a message could not be sent within the number of attempts and time agreed in the CPA
	 * 
	 * @param messageContext
	 * @param eventTypes
	 * @param maxNr
	 * @return The list of events
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name="MessageEvents")
	@WebMethod(operationName="GetMessageEvents")
	List<EbMSMessageEvent> getMessageEvents(@WebParam(name="MessageContext") @XmlElement(required=true) EbMSMessageContext messageContext, @WebParam(name="EventType") @XmlElement(required=true) EbMSMessageEventType[] eventTypes, @WebParam(name="MaxNr") Integer maxNr) throws EbMSMessageServiceException;

	/**
	 * Sets processed to true for all the current events for the message identified by messageId, so that it is no longer returned in the list of getMessageEvents (and getMessageIds)
	 * 
	 * @param messageId
	 * @throws EbMSMessageServiceException
	 */
	@WebMethod(operationName="ProcessMessageEvent")
	void processMessageEvent(@WebParam(name="MessageId") @XmlElement(required=true) String messageId) throws EbMSMessageServiceException;

}
