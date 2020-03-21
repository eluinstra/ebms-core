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
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "EbMSMessageContent")
public class EbMSMessageContentMTOM implements Serializable
{
	private static final long serialVersionUID = 1L;
	private EbMSMessageContext context;
	private List<EbMSDataSourceMTOM> dataSources;

	public EbMSMessageContentMTOM()
	{
	}
	
	public EbMSMessageContentMTOM(EbMSMessageContext context)
	{
		this(context,new ArrayList<>());
	}

	public EbMSMessageContentMTOM(EbMSMessageContext context, List<EbMSDataSourceMTOM> dataSources)
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
	
	@XmlElement(name="dataSource")
	public List<EbMSDataSourceMTOM> getDataSources()
	{
		return dataSources;
	}
	
	public void setDataSources(List<EbMSDataSourceMTOM> dataSources)
	{
		this.dataSources = dataSources;
	}
	
}
