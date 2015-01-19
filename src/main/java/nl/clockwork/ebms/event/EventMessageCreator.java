package nl.clockwork.ebms.event;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jms.core.MessageCreator;

public class EventMessageCreator implements MessageCreator
{
	private String messageId;

	public EventMessageCreator(String messageId)
	{
		this.messageId = messageId;
	}

	@Override
	public Message createMessage(Session session) throws JMSException
	{
		Message result = session.createMessage();
		result.setStringProperty("messageId",messageId);
		return result;
	}
	
}
