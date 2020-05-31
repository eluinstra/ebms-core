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

	public EbMSThreadPoolExecutor(@NonNull Integer maxThreads)
	{
		maxThreads = maxThreads--;
		if (maxThreads <= 0)
			maxThreads = Runtime.getRuntime().availableProcessors() - 1;
		log.info(this.getClass().getName() + " using " + maxThreads + " threads");
		this.maxThreads = maxThreads;
	}

	public ThreadPoolExecutor createInstance()
	{
		return new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				1,
				TimeUnit.MINUTES,
				new ArrayBlockingQueue<>(maxThreads * 4,true),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}
}
