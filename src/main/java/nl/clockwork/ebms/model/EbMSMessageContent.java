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
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class EbMSMessageContent implements Serializable
{
	private static final long serialVersionUID = 1L;
	private EbMSMessageContext context;
	private List<EbMSDataSource> dataSources;

	public EbMSMessageContent()
	{
	}
	
	public EbMSMessageContent(EbMSMessageContext context)
	{
		this(context,new ArrayList<>());
	}

	public EbMSMessageContent(EbMSMessageContext context, List<EbMSDataSource> dataSources)
	{
		this.context = context;
		this.dataSources = dataSources;
	}

	@XmlElement(required=true)
	public EbMSMessageContext getContext()
	{
		return context;
	}
	
	public void setContext(EbMSMessageContext context)
	{
		this.context = context;
	}
	
	@XmlElement(name="dataSource",required=true)
	public List<EbMSDataSource> getDataSources()
	{
		return dataSources;
	}
	
	public void setDataSources(List<EbMSDataSource> dataSources)
	{
		this.dataSources = dataSources;
	}
	
}
