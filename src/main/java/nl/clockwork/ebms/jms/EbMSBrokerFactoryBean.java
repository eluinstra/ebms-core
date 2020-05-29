package nl.clockwork.ebms.jms;

import java.io.IOException;

import org.apache.activemq.xbean.BrokerFactoryBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class EbMSBrokerFactoryBean extends BrokerFactoryBean implements DisposableBean
{
	private static BrokerFactoryBean brokerFactoryBean;

	public static void init(boolean jmsBrokerStart, String jmsBrokerConfig) throws Exception
	{
		if (brokerFactoryBean == null && jmsBrokerStart)
		{
			brokerFactoryBean = new BrokerFactoryBean(createResource(jmsBrokerConfig));
			brokerFactoryBean.setStart(jmsBrokerStart);
			brokerFactoryBean.afterPropertiesSet();
		}
	}

	private static Resource createResource(String path) throws IOException
	{
		if (path.startsWith("classpath:"))
			return new ClassPathResource(path.substring("classpath:".length()));
		else if (path.startsWith("file:"))
			return new FileSystemResource(path.substring("file:".length()));
		else
			return new FileSystemResource(path);
	}
}
