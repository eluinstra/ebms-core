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

import java.io.Serializable;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "EbMSDataSource")
public class EbMSDataSourceMTOM implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String contentId;
	private DataHandler attachment;

	public EbMSDataSourceMTOM()
	{
	}
	
	public EbMSDataSourceMTOM(DataHandler attachment)
	{
		this(null,attachment);
	}

	public EbMSDataSourceMTOM(String contentId, DataHandler attachment)
	{
		this.contentId = contentId;
		this.attachment = attachment;
	}
	
	public String getContentId()
	{
		return contentId;
	}

	public void setContentId(String contentId)
	{
		this.contentId = contentId;
	}

	@XmlMimeType("application/octet-stream")
	public DataHandler getAttachment()
	{
		return attachment;
	}

	public void setAttachment(DataHandler attachment)
	{
		this.attachment = attachment;
	}
}
