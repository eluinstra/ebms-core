package nl.clockwork.ebms;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception>
{
	void accept(T t) throws E;

	static <T, E extends Exception> Consumer<T> throwingConsumerWrapper(ThrowingConsumer<T, E> throwingConsumer) throws E
	{
		return t ->
		{
			try
			{
				throwingConsumer.accept(t);
			}
			catch (Exception ex)
			{
				throwCheckedUnchecked(ex);
			}
		};
	}

	@SuppressWarnings("unchecked")
	static <X extends Throwable> void throwCheckedUnchecked(Throwable t) throws X
	{
		throw (X) t;
	}
}