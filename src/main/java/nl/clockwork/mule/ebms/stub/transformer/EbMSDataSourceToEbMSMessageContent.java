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
package nl.clockwork.mule.ebms.stub.transformer;

import java.util.List;

import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;

public class EbMSDataSourceToEbMSMessageContent extends AbstractMessageTransformer
{
	private String cpaId;
	private String service;
	private String action;

	public EbMSDataSourceToEbMSMessageContent()
	{
		registerSourceType(DataTypeFactory.create(List.class));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			EbMSMessageContext messageContext = new EbMSMessageContext();
			messageContext.setCpaId(cpaId);
			messageContext.setService(service);
			messageContext.setAction(action);
			return new EbMSMessageContent(messageContext,(List<EbMSDataSource>)message.getPayload());
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}
	
	public void setCpaId(String cpaId)
	{
		this.cpaId = cpaId;
	}
	
	public void setService(String service)
	{
		this.service = service;
	}
	
	public void setAction(String action)
	{
		this.action = action;
	}

}