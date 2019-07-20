package nl.clockwork.ebms;

import java.util.Optional;
import java.util.function.Consumer;

public class StreamUtils
{
	public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction)
	{
		if (optional.isPresent())
			action.accept(optional.get());
		else
			emptyAction.run();
	}

	public static <T> void ifNotPresent(Optional<T> optional, Runnable emptyAction)
	{
		if (!optional.isPresent())
			emptyAction.run();
	}
}
