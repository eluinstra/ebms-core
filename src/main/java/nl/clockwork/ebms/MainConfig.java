package nl.clockwork.ebms;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.val;
import nl.clockwork.ebms.cache.CacheConfig;
import nl.clockwork.ebms.client.ClientConfig;
import nl.clockwork.ebms.cpa.CPAManagerConfig;
import nl.clockwork.ebms.dao.DAOConfig;
import nl.clockwork.ebms.dao.DataSourceConfig;
import nl.clockwork.ebms.encryption.EncryptionConfig;
import nl.clockwork.ebms.event.listener.EventListenerConfig;
import nl.clockwork.ebms.event.processor.EventManagerConfig;
import nl.clockwork.ebms.event.processor.EventProcessorConfig;
import nl.clockwork.ebms.jms.JMSConfig;
import nl.clockwork.ebms.processor.EbMSProcessorConfig;
import nl.clockwork.ebms.security.KeyStoreConfig;
import nl.clockwork.ebms.server.ServerConfig;
import nl.clockwork.ebms.service.ServiceConfig;
import nl.clockwork.ebms.signing.SigningConfig;
import nl.clockwork.ebms.validation.ValidationConfig;

@Configuration(proxyBeanMethods = false)
@Import({
	CacheConfig.class,
	ClientConfig.class,
	CommonConfig.class,
	CPAManagerConfig.class,
	DAOConfig.class,
	DataSourceConfig.class,
	EbMSProcessorConfig.class,
	EncryptionConfig.class,
	EventListenerConfig.class,
	EventManagerConfig.class,
	EventProcessorConfig.class,
	JMSConfig.class,
	KeyStoreConfig.class,
	ServerConfig.class,
	ServiceConfig.class,
	SigningConfig.class,
	ValidationConfig.class
	})
public class MainConfig
{
	@Bean
	public PropertySourcesPlaceholderConfigurer properties()
	{
		val c = new PropertySourcesPlaceholderConfigurer();
		val resources = new Resource[]{
				new ClassPathResource("nl/clockwork/ebms/default.properties")};
		c.setLocations(resources);
		c.setIgnoreUnresolvablePlaceholders(true);
		return c;
	}

	public static void main(String[] args)
	{
		try(AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class))
		{
			
		}
	}
}
