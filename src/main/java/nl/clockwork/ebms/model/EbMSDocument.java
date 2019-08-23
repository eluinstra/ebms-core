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
package nl.clockwork.ebms.model;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

public class EbMSDocument
{
	private String contentId;
	private Document message;
	private List<EbMSAttachment> attachments = new ArrayList<>();
	
	protected EbMSDocument()
	{
	}

	public EbMSDocument(String contentId, Document message)
	{
		this(contentId,message,new ArrayList<>());
	}

	public EbMSDocument(String contentId, Document message, List<EbMSAttachment> attachments)
	{
		this.contentId = contentId;
		this.message = message;
		this.attachments = attachments;
	}

	public String getContentId()
	{
		return contentId;
	}

	public Document getMessage()
	{
		return message;
	}

	public List<EbMSAttachment> getAttachments()
	{
		return attachments;
	}

}
