package nl.clockwork.ebms.event.processor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Future;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
public class EventTaskExecutor
{
	int maxEvents;
	@NonNull
	EbMSEventDAO ebMSEventDAO;
	@NonNull
	EventHandler eventHandler;
	String serverId;

	@Async
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public void handleEvents()
	{
  	while (true)
		{
			val futures = new ArrayList<Future<?>>();
			try
			{
				val timestamp = Instant.now();
				val events = maxEvents > 0 ? ebMSEventDAO.getEventsBefore(timestamp,serverId,maxEvents) : ebMSEventDAO.getEventsBefore(timestamp,serverId);
				for (EbMSEvent event : events)
					futures.add(eventHandler.handleAsync(event));
			}
			catch (Exception e)
			{
				log.error("",e);
			}
			futures.forEach(f -> Try.of(() -> f.get()).onFailure(e -> log.error("",e)));
		}
	}
}
