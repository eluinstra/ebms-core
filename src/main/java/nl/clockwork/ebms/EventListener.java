package nl.clockwork.ebms;

public interface EventListener
{
	void onMessageReceived(String messageId) throws EventException;
	void onMessageDelivered(String messageId) throws EventException;
	void onMessageDeliveryFailed(String messageId) throws EventException;
}
