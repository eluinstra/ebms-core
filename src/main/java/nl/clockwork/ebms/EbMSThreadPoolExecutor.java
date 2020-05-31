package nl.clockwork.ebms;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSThreadPoolExecutor
{
	@NonNull
	Integer maxThreads;
	@NonNull
	Integer queueScaleFactor;

	public EbMSThreadPoolExecutor(@NonNull Integer maxThreads, @NonNull Integer processorsScaleFactor, @NonNull Integer queueScaleFactor)
	{
		if (processorsScaleFactor <= 0)
		{
			processorsScaleFactor = 1;
			log.info(this.getClass().getName() + " using processors scale factor " + processorsScaleFactor);
		}
		maxThreads = maxThreads--;
		if (maxThreads <= 0)
		{
			maxThreads = Runtime.getRuntime().availableProcessors() * processorsScaleFactor - 1;
			log.info(this.getClass().getName() + " using " + maxThreads + " threads");
		}
		if (queueScaleFactor <= 0)
		{
			queueScaleFactor = 1;
			log.info(this.getClass().getName() + " using queue scale factor " + queueScaleFactor);
		}
		this.maxThreads = maxThreads;
		this.queueScaleFactor = queueScaleFactor;
	}

	public ThreadPoolExecutor createInstance()
	{
		return new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				1,
				TimeUnit.MINUTES,
				new ArrayBlockingQueue<>(maxThreads * queueScaleFactor,true),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}
}
