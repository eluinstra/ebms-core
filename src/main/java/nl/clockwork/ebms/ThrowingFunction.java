package nl.clockwork.ebms;

import java.util.function.Function;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception>
{
	R apply(T t) throws E;

	static <T, R, E extends Exception> Function<T, R> throwingFunctionWrapper(ThrowingFunction<T, R, E> throwingFunction) throws E
	{
		return t ->
		{
			try
			{
				return throwingFunction.apply(t);
			}
			catch (Exception ex)
			{
				throwCheckedUnchecked(ex);
				return null;
			}
		};
	}

	@SuppressWarnings("unchecked")
	static <X extends Throwable> void throwCheckedUnchecked(Throwable t) throws X
	{
		throw (X) t;
	}
}
