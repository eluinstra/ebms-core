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
package nl.clockwork.ebms.server.embedded.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import nl.clockwork.ebms.server.message.model.core.CPA;
import nl.clockwork.ebms.server.message.model.core.EbMSAttachment;
import nl.clockwork.ebms.server.message.model.core.EbMSMessage;
import nl.clockwork.ebms.server.message.model.embedded.web.EbMSMessageFilter;
import nl.clockwork.ebms.server.message.model.embedded.web.TimeUnit;
import org.apache.commons.csv.CSVPrinter;

public interface EbMSDAO
{
	CPA findCPA(String cpaId);

	long countCPAs();

	List<String> selectCPAIds();

	List<CPA> selectCPAs(long first, long count);

	EbMSMessage findMessage(String messageId);

	boolean existsResponseMessage(String messageId);

	EbMSMessage findResponseMessage(String messageId);

	long countMessages(EbMSMessageFilter filter);

	List<EbMSMessage> selectMessages(EbMSMessageFilter filter, long first, long count);

	EbMSAttachment findAttachment(String messageId, String contentId);

	List<String> selectMessageIds(String cpaId, String fromRole, String toRole, EbMSMessageStatus...status);

	Map<Integer, Integer> selectMessageTraffic(LocalDateTime from, LocalDateTime to, TimeUnit timeUnit, EbMSMessageStatus...status);

	void writeMessageToZip(String messageId, ZipOutputStream stream);

	void printMessagesToCSV(CSVPrinter printer, EbMSMessageFilter filter);
}
