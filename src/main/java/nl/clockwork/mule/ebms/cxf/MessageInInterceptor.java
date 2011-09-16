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
package nl.clockwork.mule.ebms.cxf;

import java.io.IOException;
import java.io.InputStream;

import nl.clockwork.common.util.Utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class MessageInInterceptor extends AbstractPhaseInterceptor<Message>
{
  protected transient Log logger = LogFactory.getLog(getClass());

	public MessageInInterceptor()
	{
		super(Phase.RECEIVE);
	}

	public MessageInInterceptor(String phase)
	{
		super(phase);
	}

	public void handleMessage(Message message) throws Fault
	{
		final StringBuilder buffer = new StringBuilder();
		buffer.append(message.get(Message.CONTENT_TYPE)).append("\n");
		InputStream is = message.getContent(InputStream.class);
		if (is != null)
		{
			CachedOutputStream bos = new CachedOutputStream();
			try
			{
				IOUtils.copy(is,bos);
				bos.flush();
				is.close();
				message.setContent(InputStream.class,bos.getInputStream());
				bos.writeCacheTo(buffer);
				bos.close();
			}
			catch (IOException e)
			{
				throw new Fault(e);
			}
		}
		try
		{
			MessageManager.set(Utils.zip(buffer.toString()));
		}
		catch (IOException e)
		{
			if (logger.isInfoEnabled())
				logger.error("",e);
			else
				logger.error(buffer.toString(),e);
		}
		if (logger.isInfoEnabled())
			logger.info(buffer.toString());
	}

}
