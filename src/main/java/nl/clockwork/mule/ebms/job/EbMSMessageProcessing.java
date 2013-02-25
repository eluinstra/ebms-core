package nl.clockwork.mule.ebms.job;

import java.util.List;

import nl.clockwork.ebms.iface.EbMSMessageService;
import nl.clockwork.ebms.job.Job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.module.client.MuleClient;

public class EbMSMessageProcessing implements Job, MuleContextAware
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private MuleContext muleContext;
	private EbMSMessageService ebMSMessageService;
	private String delegatePath;

	@Override
	public void execute()
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
			logger.error("",e);
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
