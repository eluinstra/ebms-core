/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.iface;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.MessageStatus;

@WebService(targetNamespace="http://www.clockwork.nl/ebms/1.0")
public interface EbMSMessageService
{
	@WebMethod(operationName="Ping")
	void ping(String cpaId, String fromRole, String toRole) throws EbMSMessageServiceException;
	
	@WebResult(name="MessageId")
	@WebMethod(operationName="SendMessage")
	String sendMessage(@WebParam(name="Message") EbMSMessageContent messageContent) throws EbMSMessageServiceException;

	@WebResult(name="MessageId")
	@WebMethod(operationName="GetMessageIds")
	List<String> getMessageIds(@WebParam(name="MessageContext") EbMSMessageContext messageContext, @WebParam(name="MaxNr") Integer maxNr) throws EbMSMessageServiceException;

	@WebResult(name="Message")
	@WebMethod(operationName="GetMessage")
	EbMSMessageContent getMessage(@WebParam(name="MessageId") String messageId, @WebParam(name="Process") Boolean process) throws EbMSMessageServiceException;

	//@WebResult(name="Message")
	//@WebMethod(operationName="GetRefToMessage")
	//EbMSMessageContent getRefToMessage(@WebParam(name="MessageId") String messageId, @WebParam(name="Process") Boolean process) throws EbMSMessageServiceException;

	@WebResult(name="Result")
	@WebMethod(operationName="ProcessMessage")
	boolean processMessage(@WebParam(name="MessageId") String messageId) throws EbMSMessageServiceException;

	@WebResult(name="Result")
	@WebMethod(operationName="ProcessMessages")
	boolean processMessages(@WebParam(name="MessageId") List<String> messageIds) throws EbMSMessageServiceException;

	@WebResult(name="MessageId")
	@WebMethod(operationName="Ping")
	MessageStatus getMessageStatus(String cpaId, String fromRole, String toRole, String messageId) throws EbMSMessageServiceException;
	
}
