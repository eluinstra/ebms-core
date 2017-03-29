package nl.clockwork.ebms.event;

import java.util.HashMap;

import javax.jms.Destination;

import nl.clockwork.ebms.dao.EbMSDAO;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jms.core.JmsTemplate;

public class EventListenerFactory implements FactoryBean<EventListener>
{
	enum EventListenerType
	{
		DEFAULT, LOGGING, DAO, SIMPLE_JMS, JMS;
	}
	
	private EventListenerType type;
	private EbMSDAO ebMSDAO;
	private String jmsBrokerURL;

	@Override
	public EventListener getObject() throws Exception
	{
		if (EventListenerType.LOGGING.equals(type))
		{
			return new LoggingEventListener();
		}
		else if (EventListenerType.DAO.equals(type))
		{
			return new DAOEventListener(ebMSDAO);
		}
		else if (EventListenerType.SIMPLE_JMS.equals(type))
		{
			return new SimpleJMSEventListener(createJmsTemplate(jmsBrokerURL),createDestinations());
		}
		else if (EventListenerType.LOGGING.equals(type))
		{
			return new JMSEventListener(ebMSDAO,createJmsTemplate(jmsBrokerURL),createDestinations());
		}
		else
		{
			return new DefaultEventListener();
		}
	}

	@Override
	public Class<?> getObjectType()
	{
		return EventListener.class;
	}

	@Override
	public boolean isSingleton()
	{
		return false;
	}

	private JmsTemplate createJmsTemplate(String jmsBrokerURL)
	{
		PooledConnectionFactory pooledConnectionFactory = createConnectionFactory(jmsBrokerURL);
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(pooledConnectionFactory);
		return jmsTemplate;
	}

	private PooledConnectionFactory createConnectionFactory(String jmsBrokerURL)
	{
		PooledConnectionFactory result = new PooledConnectionFactory();
		result.setConnectionFactory(createActiveMQConnectionFactory(jmsBrokerURL));
		return result;
	}

	private ActiveMQConnectionFactory createActiveMQConnectionFactory(String jmsBrokerURL)
	{
		ActiveMQConnectionFactory result = new ActiveMQConnectionFactory();
		result.setBrokerURL(jmsBrokerURL);
		return result;
	}

	private HashMap<String,Destination> createDestinations()
	{
		HashMap<String,Destination> result = new HashMap<String,Destination>();
		result.put(nl.clockwork.ebms.Constants.EbMSMessageEventType.RECEIVED.toString(),new ActiveMQQueue(nl.clockwork.ebms.Constants.EbMSMessageEventType.RECEIVED.toString()));
		result.put(nl.clockwork.ebms.Constants.EbMSMessageEventType.ACKNOWLEDGED.toString(),new ActiveMQQueue(nl.clockwork.ebms.Constants.EbMSMessageEventType.ACKNOWLEDGED.toString()));
		result.put(nl.clockwork.ebms.Constants.EbMSMessageEventType.FAILED.toString(),new ActiveMQQueue(nl.clockwork.ebms.Constants.EbMSMessageEventType.FAILED.toString()));
		result.put(nl.clockwork.ebms.Constants.EbMSMessageEventType.EXPIRED.toString(),new ActiveMQQueue(nl.clockwork.ebms.Constants.EbMSMessageEventType.EXPIRED.toString()));
		return result;
	}

	public void setType(EventListenerType type)
	{
		this.type = type;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setJmsBrokerURL(String jmsBrokerURL)
	{
		this.jmsBrokerURL = jmsBrokerURL;
	}
}
