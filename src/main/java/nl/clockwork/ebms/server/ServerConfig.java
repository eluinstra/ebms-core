package nl.clockwork.ebms.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerConfig
{
	@Autowired
	EbMSMessageProcessor messageProcessor;

	@Bean
	public EbMSHttpHandler httpHandler()
	{
		return new EbMSHttpHandler(messageProcessor);
	}
}
