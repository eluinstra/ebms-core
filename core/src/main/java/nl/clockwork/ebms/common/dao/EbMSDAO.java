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
package nl.clockwork.ebms.common.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import nl.clockwork.ebms.common.EbMSAction;
import nl.clockwork.ebms.common.EbMSMessageStatus;
import nl.clockwork.ebms.common.model.EbMSAttachment;
import nl.clockwork.ebms.common.model.EbMSBaseMessage;
import nl.clockwork.ebms.common.model.EbMSDocument;
import nl.clockwork.ebms.common.model.EbMSMessageProperties;
import org.w3c.dom.Document;

public interface EbMSDAO
{
	void executeTransaction(Runnable runnable);

	boolean existsMessage(String messageId);

	boolean existsIdenticalMessage(EbMSBaseMessage message);

	Optional<EbMSMessageProperties> getEbMSMessageProperties(String messageId);

	Optional<EbMSMessageProperties> getEbMSMessagePropertiesByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);

	Optional<Document> getDocument(String messageId);

	Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);

	Optional<Instant> getPersistTime(String messageId);

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
