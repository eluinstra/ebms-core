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
package nl.clockwork.ebms.common.cxf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.clockwork.ebms.AttachmentManager;
import nl.clockwork.ebms.model.EbMSAttachment;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.phase.Phase;

public class AttachmentOutInterceptor extends AbstractSoapInterceptor
{
	public AttachmentOutInterceptor()
	{
		super(Phase.USER_LOGICAL);
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault
	{
		List<EbMSAttachment> ebMSAttachments = AttachmentManager.get();
		Collection<Attachment> attachments = new ArrayList<Attachment>();
		for (EbMSAttachment attachment : ebMSAttachments)
			attachments.add(new nl.clockwork.ebms.common.cxf.Attachment(attachment.getContentId(),attachment));
		message.setAttachments(attachments);
	}

}
