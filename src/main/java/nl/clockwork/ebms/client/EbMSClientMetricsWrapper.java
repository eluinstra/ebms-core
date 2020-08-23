package nl.clockwork.ebms.client;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.metrics.MetricsService;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSClientMetricsWrapper implements EbMSClient
{
	@NonNull
	MetricsService metricsService;
	@NonNull
	EbMSClient ebMSClient;

	@Override
	public EbMSDocument sendMessage(String uri, EbMSDocument message) throws EbMSProcessorException
	{
		val result = ebMSClient.sendMessage(uri,message);
		metricsService.increment("EbMSClient.sendMessage");
		return result;
	}
}
