package nl.clockwork.ebms.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetricsConfig
{
	@Bean
	public MetricsService getMetricsService()
	{
		return new DummyMetricsService();
	}
}
