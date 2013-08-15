package nl.clockwork.ebms;

public interface EventHandler
{
	void onMessageReceived(String messageId) throws EventException;
	void onMessageDelivered(String messageId) throws EventException;
}
