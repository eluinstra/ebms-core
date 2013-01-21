package nl.clockwork.mule.ebms.job;

import nl.clockwork.ebms.job.ProcessSendEvents;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public class ProcessSendEventsJob implements StatefulJob
{
	private ProcessSendEvents processSendEvents;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException
	{
		processSendEvents.process();
	}

	public void setProcessSendEvents(ProcessSendEvents processSendEvents)
	{
		this.processSendEvents = processSendEvents;
	}
}
