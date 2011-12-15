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
package nl.clockwork.mule.ebms.stub.ebf.filter;

import javax.activation.DataSource;

import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.stub.ebf.model.afleveren.bericht.AfleverBericht;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class AfleverBerichtBerichtSoortValidationFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private String berichtSoort;

	@Override
	public boolean accept(MuleMessage message)
	{
		if (message.getPayload() instanceof EbMSMessageContent)
		{
			try
			{
				EbMSMessageContent content = (EbMSMessageContent)message.getPayload();
				DataSource attachment = content.getAttachments().iterator().next();
				AfleverBericht afleverBericht = XMLMessageBuilder.getInstance(AfleverBericht.class).handle(attachment.getInputStream());
				return berichtSoort.equalsIgnoreCase(afleverBericht.getBerichtsoort());
			}
			catch (Exception e)
			{
				logger.error(e);
				return false;
			}
		}
		return true;
	}

	public void setBerichtSoort(String berichtSoort)
	{
		this.berichtSoort = berichtSoort;
	}
}
