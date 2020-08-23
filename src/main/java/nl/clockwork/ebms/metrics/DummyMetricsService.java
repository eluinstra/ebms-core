package nl.clockwork.ebms.metrics;

public class DummyMetricsService implements MetricsService
{
	@Override
	public void increment(String metricName)
	{
	}

	@Override
	public void decrement(String metricName)
	{
	}

	@Override
	public void reset(String metricName)
	{
	}
}
