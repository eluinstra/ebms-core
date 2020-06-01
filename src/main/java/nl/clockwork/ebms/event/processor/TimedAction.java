package nl.clockwork.ebms.event.processor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.Action;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class TimedAction
{
	Action action;
	long executionInterval;
	
	public void run()
	{
		if (executionInterval > 0)
		{
			val start = Instant.now();
			action.run();
			val end = Instant.now();
			val sleep = executionInterval - ChronoUnit.MILLIS.between(start,end);
			sleep(sleep);
		}
		else
			action.run();
	}

	private void sleep(long millis)
	{
		try
		{
			if (millis > 0)
				Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			log.trace("",e);
		}
	}
}
