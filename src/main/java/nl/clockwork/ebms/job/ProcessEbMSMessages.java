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
package nl.clockwork.ebms.job;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nl.clockwork.ebms.iface.EbMSMessageService;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.processor.EbMSProcessMessageCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessEbMSMessages implements Job
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private ExecutorService executorService;
	private int maxThreads = 4;
  private EbMSMessageService ebMSMessageService;
  private EbMSProcessMessageCallback ebMSProcessMessageCallback;
  
	public void init()
	{
		executorService = Executors.newFixedThreadPool(maxThreads);
	}
	
	@Override
	public void execute()
	{
		List<String> messageIds = ebMSMessageService.getMessageIds(null,null);
		for (final String messageId : messageIds)
		{
			executorService.execute(
				new Runnable()
				{
					
					@Override
					public void run()
					{
						EbMSMessageContent messageContent = ebMSMessageService.getMessage(messageId,false);
						ebMSProcessMessageCallback.process(messageContent);
						ebMSMessageService.processMessage(messageId);
					}
				}
			);
		}
	}

	public void setMaxThreads(int maxThreads)
	{
		this.maxThreads = maxThreads;
	}
	
	public void setEbMSMessageService(EbMSMessageService ebMSMessageService)
	{
		this.ebMSMessageService = ebMSMessageService;
	}

	public void setEbMSProcessMessageCallback(EbMSProcessMessageCallback ebMSProcessMessageCallback)
	{
		this.ebMSProcessMessageCallback = ebMSProcessMessageCallback;
	}
}
