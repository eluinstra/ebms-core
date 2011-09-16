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
package nl.clockwork.mule.ebms.service.stub.transformer;

import java.util.List;

import javax.activation.DataSource;

import nl.clockwork.mule.ebms.model.EbMSMessageContent;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class DataSourceToEbMSMessageContent extends AbstractMessageAwareTransformer
{
	public DataSourceToEbMSMessageContent()
	{
		registerSourceType(List.class);
	}
	
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			List<DataSource> ds = (List<DataSource>)message.getPayload();
			EbMSMessageContent result = new EbMSMessageContent(ds);
			return result;
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}

}
