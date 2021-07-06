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
package nl.clockwork.ebms.event.listener;

import java.util.List;

import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;

public interface EbMSMessageEventDAO
{
	List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types);
	List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types, int maxNr);
	String insertEbMSMessageEvent(String messageId, EbMSMessageEventType eventType);
	int processEbMSMessageEvent(String messageId);
}