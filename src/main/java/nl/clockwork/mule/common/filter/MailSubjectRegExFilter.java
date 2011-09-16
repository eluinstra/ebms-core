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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.routing.filters.RegExFilter;
import org.mule.util.ClassUtils;

public class MailSubjectRegExFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());

  private RegExFilter filter = new RegExFilter();
	
	public boolean accept(Message message)
	{
		try
		{
			return filter.accept(message.getSubject());
		}
		catch (MessagingException e)
		{
			logger.warn("",e);
			return false;
		}
	}
	
	@Override
	public boolean accept(MuleMessage message)
	{
		Object object = message.getPayload();
		if (object instanceof Message)
		{
			return accept((Message)object);
		}
		else if (object instanceof MimeBodyPart)
		{
			return filter.accept(message.getProperty("Subject"));
		}
		else
		{
			return false;
		}
	}

	public void setExpression(String pattern)
	{
		filter.setPattern(pattern);
	}
	
	public String getExpression()
	{
		return filter.getPattern();
	}
	
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
	
		final MailSubjectRegExFilter other = (MailSubjectRegExFilter) obj;
		return ClassUtils.equal(filter, other.filter);
	}
	
	public int hashCode()
	{
		return ClassUtils.hash(new Object[]{this.getClass(), filter});
	}

}
