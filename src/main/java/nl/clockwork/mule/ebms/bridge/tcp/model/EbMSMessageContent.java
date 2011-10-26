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
package nl.clockwork.mule.ebms.bridge.tcp.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EbMSMessageContent implements Serializable
{
	private static final long serialVersionUID = 1L;
	private EbMSMessageContext context;
	private Map<String,Object> properties = new HashMap<String,Object>();
	private List<EbMSDataSource> attachments;

	public EbMSMessageContent(EbMSMessageContext context, Map<String,Object> properties, List<EbMSDataSource> attachments)
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

	public List<EbMSDataSource> getAttachments()
	{
		return attachments;
	}
	
}
