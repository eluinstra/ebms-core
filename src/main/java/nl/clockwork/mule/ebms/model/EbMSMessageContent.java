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
package nl.clockwork.mule.ebms.model;

import java.util.List;

public class EbMSMessageContent
{
	private static final long serialVersionUID = 1L;
	private EbMSMessageContext context;
	private List<EbMSAttachment> attachments;

	public EbMSMessageContent()
	{
	}
	
	public EbMSMessageContent(EbMSMessageContext context, List<EbMSAttachment> attachments)
	{
		this.context = context;
		this.attachments = attachments;
	}

	public EbMSMessageContext getContext()
	{
		return context;
	}
	
	public void setContext(EbMSMessageContext context)
	{
		this.context = context;
	}
	
	public List<EbMSAttachment> getAttachments()
	{
		return attachments;
	}
	
	public void setAttachments(List<EbMSAttachment> attachments)
	{
		this.attachments = attachments;
	}
	
}
