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
package nl.clockwork.mule.ebms.adapter.service;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import nl.clockwork.mule.ebms.model.EbMSMessageContent;

@WebService(targetNamespace="http://www.clockwork.nl/ebms/adapter/1.0")
public interface EbMSAdapter
{
	@WebResult(name="MessageId")
	String sendMessage(@WebParam(name="Message") EbMSMessageContent messageContent);

	@WebResult(name="MessageIds")
	List<String> getMessageIds(@WebParam(name="MaxNr") int maxNr);

	@WebResult(name="Message")
	EbMSMessageContent getMessage(@WebParam(name="MessageId") String messageId, @WebParam(name="Process") boolean process);

	@WebResult(name="Result")
	boolean processMessage(@WebParam(name="MessageId") String messageId);

	@WebResult(name="Result")
	boolean processMessages(@WebParam(name="MessageIds") List<String> messageIds);

}
