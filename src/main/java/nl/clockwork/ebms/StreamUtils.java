package nl.clockwork.ebms;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	
	public static IllegalStateException illegalStateException(String message, Object...elements)
	{
		return new IllegalStateException(message + ": "+ Stream.of(elements).map(o -> o.toString()).collect(Collectors.joining(",")));
	}
}
