package nl.clockwork.mule.ebms.job;

import java.util.List;

import nl.clockwork.ebms.iface.EbMSMessageService;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.module.client.MuleClient;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public class EbMSMessageProcessingJob implements StatefulJob, MuleContextAware
{
	private MuleContext muleContext;
	private EbMSMessageService ebMSMessageService;
	private String delegatePath;

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException
	{
		try
		{
			MuleClient client = new MuleClient(muleContext);
			List<String> messageIds = ebMSMessageService.getMessageIds(null,null);
			for (String messageId : messageIds)
			{
				client.send(delegatePath,ebMSMessageService.getMessage(messageId,false),null);
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
