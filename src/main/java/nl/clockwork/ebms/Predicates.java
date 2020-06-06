package nl.clockwork.ebms;

import java.util.function.Predicate;

public final class Predicates
{
	public static Predicate<String> startsWith(String value)
	{
		return obj -> obj.startsWith(value);
	}

	public static Predicate<String> endsWith(String value)
	{
		return obj -> obj.endsWith(value);
	}
}
