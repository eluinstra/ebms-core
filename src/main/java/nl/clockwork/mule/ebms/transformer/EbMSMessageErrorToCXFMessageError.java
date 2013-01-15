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
package nl.clockwork.mule.ebms.transformer;

import nl.clockwork.ebms.model.EbMSMessageError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageErrorToCXFMessageError extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());

  public EbMSMessageErrorToCXFMessageError()
	{
		registerSourceType(EbMSMessageError.class);
	}
  
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		EbMSMessageError messageError = (EbMSMessageError)message.getPayload();
		message.setPayload(new Object[]{messageError.getMessageHeader(),messageError.getErrorList()});
		return message;
	}

}
