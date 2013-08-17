package nl.clockwork.ebms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoggingEventListener implements EventListener
{
	protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		logger.info("Message received. MessageId: " + messageId);
	}

	@Override
	public void onMessageDelivered(String messageId) throws EventException
	{
		logger.info("Message delivered. MessageId: " + messageId);
	}
	
	@Override
	public void onMessageDeliveryFailed(String messageId) throws EventException
	{
		logger.info("Message delivery failed. MessageId: " + messageId);
	}
}
