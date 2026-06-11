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
package nl.clockwork.ebms.api.ebms;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import nl.clockwork.ebms.api.ebms.model.MTOMMessage;
import nl.clockwork.ebms.api.ebms.model.Message;
import nl.clockwork.ebms.api.ebms.model.MessageFilter;
import nl.clockwork.ebms.common.model.EbMSAttachment;
import nl.clockwork.ebms.common.model.EbMSBaseMessage;
import nl.clockwork.ebms.common.model.EbMSMessageProperties;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import org.w3c.dom.Document;

public interface EbMSDAO
{
	void executeTransaction(Runnable runnable);

	Optional<EbMSMessageProperties> getEbMSMessageProperties(String messageId);

	Optional<Message> getMessage(String messageId);

	Optional<MTOMMessage> getMTOMMessage(String messageId);

	List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status);

	List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status, int maxNr);

	String insertMessage(
			Instant timestamp,
			Instant persistTime,
			Document document,
			EbMSBaseMessage message,
			List<EbMSAttachment> attachments,
			EbMSMessageStatus status);

	int updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus);

	int deleteAttachments(String messageId);
}
