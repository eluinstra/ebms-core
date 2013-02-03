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
package nl.clockwork.ebms.model;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

public class EbMSResponseDocument extends EbMSDocument
{
	private EbMSDocument ebMSDocument;
	private Integer statusCode;
	
	public EbMSResponseDocument(EbMSDocument ebMSDocument, Integer statusCode)
	{
		super();
		this.ebMSDocument = ebMSDocument;
		this.statusCode = statusCode;
	}

	@Override
	public Document getMessage()
	{
		return ebMSDocument == null ? null : ebMSDocument.getMessage();
	}
	
	@Override
	public List<EbMSAttachment> getAttachments()
	{
		return ebMSDocument == null ? new ArrayList<EbMSAttachment>() : ebMSDocument.getAttachments();
	}
	
	public Integer getStatusCode()
	{
		return statusCode;
	}
	
	public void setStatusCode(Integer statusCode)
	{
		this.statusCode = statusCode;
	}

}
