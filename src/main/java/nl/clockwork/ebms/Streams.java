package nl.clockwork.ebms;

import java.util.function.Consumer;
import java.util.function.Function;

import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.CheckedConsumer;
import nl.clockwork.ebms.util.CheckedFunction;

public class Streams
{
	public static <T> Consumer<T> consumer1(CheckedConsumer<T> consumer)
	{
		return t -> {
			try
			{
				consumer.accept(t);
			}
			catch (Exception e)
			{
				if (e instanceof RuntimeException)
					throw (RuntimeException)e;
				else
					throw new EbMSProcessingException(e);
			}
		};
	}

	public static <T,R> Function<T,R> function1(CheckedFunction<T,R> function)
	{
		return t -> {
			try
			{
				return function.apply(t);
			}
			catch (Exception e)
			{
				if (e instanceof RuntimeException)
					throw (RuntimeException)e;
				else
					throw new EbMSProcessingException(e);
			}
		};
	}
}
