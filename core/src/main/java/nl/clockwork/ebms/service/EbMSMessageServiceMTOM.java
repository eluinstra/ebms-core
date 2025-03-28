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
package nl.clockwork.ebms.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.ws.soap.MTOM;
import java.util.List;
import nl.clockwork.ebms.event.MessageEventType;
import nl.clockwork.ebms.service.model.MTOMMessage;
import nl.clockwork.ebms.service.model.MTOMMessageRequest;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;
import nl.clockwork.ebms.service.model.MessageStatus;

@MTOM(enabled = true)
@WebService(
		name = "EbMSMessageService",
		targetNamespace = "http://www.ordina.nl/ebms/2.18",
		serviceName = "EbMSMessageService",
		endpointInterface = "EbMSMessageServiceSoapBinding",
		portName = "EbMSMessagePort")
public interface EbMSMessageServiceMTOM
{
	/**
	 * Performs an EbMS ping action for CPA cpaId, from party fromPartyId to party toPartyId
	 * 
	 * @param cpaId
	 * @param fromPartyId
	 * @param toPartyId
	 * @throws EbMSMessageServiceException
	 */
	@WebMethod(operationName = "ping")
	void ping(
			@WebParam(name = "cpaId") @XmlElement(required = true) String cpaId,
			@WebParam(name = "fromPartyId") @XmlElement(required = true) String fromPartyId,
			@WebParam(name = "toPartyId") @XmlElement(required = true) String toPartyId) throws EbMSMessageServiceException;

	/**
	 * Sends message message as an EbMS message using MTOM/XOP
	 * 
	 * @param message
	 * @return The messageId of the generated EbMS message
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name = "messageId")
	@WebMethod(operationName = "sendMessage")
	String sendMessageMTOM(@WebParam(name = "message") @XmlElement(required = true) MTOMMessageRequest message) throws EbMSMessageServiceException;

	/**
	 * Resends the message identified by messageId as an EbMS message
	 * 
	 * @param messageId
	 * @return The messageId of the generated EbMS message
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name = "messageId")
	@WebMethod(operationName = "resendMessage")
	String resendMessage(@WebParam(name = "messageId") @XmlElement(required = true) String messageId) throws EbMSMessageServiceException;

	/**
	 * Returns all messageIds of messages with status RECEIVED that satisfy filter messageFilter. If maxNr is given, then maxNr messageIds are returned
	 * 
	 * @param messageFilter
	 * @param maxNr
	 * @return The list of messageIds
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name = "messageIds")
	@WebMethod(operationName = "getUnprocessedMessageIds")
	List<String> getUnprocessedMessageIds(@WebParam(name = "messageFilter") MessageFilter messageFilter, @WebParam(name = "maxNr") Integer maxNr)
			throws EbMSMessageServiceException;

	/**
	 * Returns the message identified by messageId. If process is true, the message is given the status PROCESSED, which means that it is no longer returned in
	 * the list of getUnprocessedMessageIds
	 * 
	 * @param messageId
	 * @param process
	 * @return The message
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name = "message")
	@WebMethod(operationName = "getMessage")
	MTOMMessage getMessageMTOM(@WebParam(name = "messageId") @XmlElement(required = true) String messageId, @WebParam(name = "process") Boolean process)
			throws EbMSMessageServiceException;

	/**
	 * Sets the status of the message identified by messageId to PROCESSED, so that it is no longer returned in the list of getUnprocessedMessageIds
	 * 
	 * @param messageId
	 * @throws EbMSMessageServiceException
	 */
	@WebMethod(operationName = "processMessage")
	void processMessage(@WebParam(name = "messageId") @XmlElement(required = true) String messageId) throws EbMSMessageServiceException;

	/**
	 * Returns the message status of the message identified by messageId
	 * 
	 * @param messageId
	 * @return The message status
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name = "messageStatus")
	@WebMethod(operationName = "getMessageStatus")
	MessageStatus getMessageStatus(@WebParam(name = "messageId") @XmlElement(required = true) String messageId) throws EbMSMessageServiceException;

	/**
	 * Returns the events that satisfy filter messageFilter and event types eventTypes. If maxNr is given, then maxNr events are returned. The possible event
	 * types are: - RECEIVED - when a message is received - DELIVERED - when a message has been sent successfully - FAILED - when a message returns an error while
	 * sending - EXPIRED - when a message could not be sent within the number of attempts and time defined in the CPA
	 * 
	 * @param messageFilter
	 * @param eventTypes
	 * @param maxNr
	 * @return The list of events
	 * @throws EbMSMessageServiceException
	 */
	@WebResult(name = "messageEvents")
	@WebMethod(operationName = "getUnprocessedMessageEvents")
	List<MessageEvent> getUnprocessedMessageEvents(
			@WebParam(name = "messageFilter") MessageFilter messageFilter,
			@WebParam(name = "eventType") MessageEventType[] eventTypes,
			@WebParam(name = "maxNr") Integer maxNr) throws EbMSMessageServiceException;

	/**
	 * Sets processed to true for the event of the message identified by messageId, so that it is no longer returned in the list of getUnprocessedMessageEvents
	 * (and getUnprocessedMessageIds in case of a RECEIVED event)
	 * 
	 * @param messageId
	 * @throws EbMSMessageServiceException
	 */
	@WebMethod(operationName = "processMessageEvent")
	void processMessageEvent(@WebParam(name = "messageId") @XmlElement(required = true) String messageId) throws EbMSMessageServiceException;

}
