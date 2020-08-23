package nl.clockwork.ebms.metrics;

public interface MetricsService
{
	void increment(String metricName);
	void decrement(String metricName);
	void reset(String metricName);
}
