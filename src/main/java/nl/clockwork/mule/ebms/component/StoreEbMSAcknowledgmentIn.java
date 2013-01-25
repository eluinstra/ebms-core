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
package nl.clockwork.mule.ebms.component;

import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.mule.common.component.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;

public class StoreEbMSAcknowledgmentIn extends Callable
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;

	@Override
	public Object onCall(MuleMessage message) throws Exception
	{
		if (message.getPayload() instanceof EbMSAcknowledgment)
		{
			EbMSAcknowledgment ack = (EbMSAcknowledgment)message.getPayload();
			//FIXME quickfix to prevent inserting duplicate messages
			if (!ebMSDAO.existsMessage(ack.getMessageHeader().getMessageData().getMessageId()))
				ebMSDAO.insertMessage(ack,EbMSMessageStatus.DELIVERED);
			else
				logger.warn("Duplicate acknowlegment found.");
		}
		return message;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
