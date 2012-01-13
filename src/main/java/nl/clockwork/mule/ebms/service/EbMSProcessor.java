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
package nl.clockwork.mule.ebms.service;

import nl.clockwork.mule.ebms.model.EbMSAcknowledgment;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageError;
import nl.clockwork.mule.ebms.model.EbMSMessageStatusRequest;
import nl.clockwork.mule.ebms.model.EbMSMessageStatusResponse;
import nl.clockwork.mule.ebms.model.EbMSPing;
import nl.clockwork.mule.ebms.model.EbMSPong;


public interface EbMSProcessor
{
	void process(EbMSMessage message);
	void process(EbMSMessageError error);
	void process(EbMSAcknowledgment acknowledgment);

	EbMSMessageStatusResponse process(EbMSMessageStatusRequest request);
	EbMSPong process(EbMSPing header);
}
