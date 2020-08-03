package nl.clockwork.ebms.event.listener;

import java.util.EnumSet;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EventListenerFilter implements EventListener
{
	EnumSet<EbMSMessageEventType> filter;
	EventListener eventListener;

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		if (!filter.contains(EbMSMessageEventType.RECEIVED))
			eventListener.onMessageReceived(messageId);
	}

	@Override
	public void onMessageDelivered(String messageId) throws EventException
	{
		if (!filter.contains(EbMSMessageEventType.DELIVERED))
			eventListener.onMessageDelivered(messageId);
	}

	@Override
	public void onMessageFailed(String messageId) throws EventException
	{
		if (!filter.contains(EbMSMessageEventType.FAILED))
			eventListener.onMessageFailed(messageId);
	}

	@Override
	public void onMessageExpired(String messageId) throws EventException
	{
		if (!filter.contains(EbMSMessageEventType.EXPIRED))
			eventListener.onMessageExpired(messageId);
	}
}
