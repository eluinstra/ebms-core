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
package nl.clockwork.mule.common.filter;

import javax.activation.DataHandler;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class MimeXMLAttachmentFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public boolean accept(MuleMessage message)
	{
		Object object = message.getPayload();
		if (object instanceof MimeBodyPart)
			if (message.getAttachmentNames().size() == 1)
			{
				DataHandler dh = message.getAttachment((String)message.getAttachmentNames().iterator().next());
				return dh.getContentType().startsWith("text/xml");
			}
		return false;
	}

}
