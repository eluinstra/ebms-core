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
package nl.clockwork.mule.ebms.job;

import java.util.List;

import nl.clockwork.ebms.service.EbMSMessageService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.module.client.MuleClient;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public class EbMSMessageProcessingJob implements StatefulJob, MuleContextAware
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private MuleContext muleContext;
	private EbMSMessageService ebMSMessageService;
	private String delegatePath;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException
	{
		logger.debug("Executing job: " + this.getClass());
		try
		{
			MuleClient client = new MuleClient(muleContext);
			List<String> messageIds = ebMSMessageService.getMessageIds(null,null);
			for (String messageId : messageIds)
			{
				client.sendNoReceive(delegatePath,ebMSMessageService.getMessage(messageId,false),null);
				ebMSMessageService.processMessage(messageId);
			}
		}
		catch (MuleException e)
		{
			throw new JobExecutionException(e);
		}
	}

	@Override
	public void setMuleContext(MuleContext muleContext)
	{
		this.muleContext = muleContext;
	}
	
	public void setEbMSMessageService(EbMSMessageService ebMSMessageService)
	{
		this.ebMSMessageService = ebMSMessageService;
	}
	
	public void setDelegatePath(String delegatePath)
	{
		this.delegatePath = delegatePath;
	}
}
