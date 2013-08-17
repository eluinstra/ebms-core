package nl.clockwork.ebms;

public class DefaultEventListener implements EventListener
{
	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
	}

	@Override
	public void onMessageDelivered(String messageId) throws EventException
	{
	}
	
	@Override
	public void onMessageDeliveryFailed(String messageId) throws EventException
	{
		// TODO Auto-generated method stub
		
	}
}
