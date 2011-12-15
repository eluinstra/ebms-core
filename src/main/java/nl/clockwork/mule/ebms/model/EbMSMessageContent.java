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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;

public class EbMSMessageContent
{
	private EbMSMessageContext context;
	private Map<String,Object> properties = new HashMap<String,Object>();
	private List<DataSource> attachments;

	public EbMSMessageContent(EbMSMessageContext context, List<DataSource> attachments)
	{
		this(context,null,attachments);
	}

	public EbMSMessageContent(EbMSMessageContext context, Map<String,Object> properties, List<DataSource> attachments)
	{
		this.context = context;
		this.properties = properties == null ? new HashMap<String,Object>() : properties;
		this.attachments = attachments;
	}

	public EbMSMessageContext getContext()
	{
		return context;
	}
	
	public Map<String,Object> getProperties()
	{
		return properties;
	}

	public List<DataSource> getAttachments()
	{
		return attachments;
	}
	
}
