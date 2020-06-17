package nl.clockwork.ebms.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

public class CloseablePooledConnectionFactory extends PooledConnectionFactory implements AutoCloseable
{
	public CloseablePooledConnectionFactory()
	{
		super();
	}

	public CloseablePooledConnectionFactory(ActiveMQConnectionFactory activeMQConnectionFactory)
	{
		super(activeMQConnectionFactory);
	}

	public CloseablePooledConnectionFactory(String brokerURL)
	{
		super(brokerURL);
	}

	@Override
	public void close() throws Exception
	{
	}
}
