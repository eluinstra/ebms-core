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
package nl.clockwork.ebms.adapter.service;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;

@WebService(targetNamespace="http://www.clockwork.nl/ebms/adapter/1.0")
public interface EbMSAdapter
{
	@WebResult(name="MessageId")
	@WebMethod(operationName="SendMessage")
	String sendMessage(@WebParam(name="Message") EbMSMessageContent messageContent);

	@WebResult(name="MessageId")
	@WebMethod(operationName="GetMessageIds")
	List<String> getMessageIds(@WebParam(name="MessageContext") EbMSMessageContext messageContext, @WebParam(name="MaxNr") Integer maxNr);

	@WebResult(name="Message")
	@WebMethod(operationName="GetMessage")
	EbMSMessageContent getMessage(@WebParam(name="MessageId") String messageId, @WebParam(name="Process") Boolean process);

	//@WebResult(name="Message")
	//@WebMethod(operationName="GetRefToMessage")
	//EbMSMessageContent getRefToMessage(@WebParam(name="MessageId") String messageId, @WebParam(name="Process") Boolean process);

	@WebResult(name="Result")
	@WebMethod(operationName="ProcessMessage")
	boolean processMessage(@WebParam(name="MessageId") String messageId);

	@WebResult(name="Result")
	@WebMethod(operationName="ProcessMessages")
	boolean processMessages(@WebParam(name="MessageId") List<String> messageIds);

}
