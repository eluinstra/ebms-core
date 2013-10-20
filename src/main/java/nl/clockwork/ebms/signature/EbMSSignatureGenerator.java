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
package nl.clockwork.ebms.signature;

import java.util.List;

import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3c.dom.Document;

public interface EbMSSignatureGenerator
{
	void generate(CollaborationProtocolAgreement cpa, MessageHeader messageHeader, Document document, List<EbMSAttachment> attachments) throws EbMSProcessorException;
	void generate(CollaborationProtocolAgreement cpa, AckRequested ackRequested, MessageHeader resposeMessageHeader, Document document, List<EbMSAttachment> attachments) throws EbMSProcessorException;
}