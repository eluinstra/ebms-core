package nl.clockwork.ebms.server;

import java.io.InputStream;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.metrics.MetricsService;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class EbMSInputStreamMetricsHandler extends EbMSInputStreamHandler
{
	@NonNull
	MetricsService metricsService;

	public EbMSInputStreamMetricsHandler(@NonNull MetricsService metricsService, EbMSMessageProcessor messageProcessor)
	{
		super(messageProcessor);
		this.metricsService = metricsService;
	}

	@Override
	public void handle(InputStream request)
	{
		super.handle(request);
		metricsService.increment("EbMSServer.handleRequest");
	}
}
