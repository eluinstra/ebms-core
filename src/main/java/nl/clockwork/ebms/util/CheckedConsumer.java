package nl.clockwork.ebms.util;

@FunctionalInterface
public interface CheckedConsumer <T>
{
	void accept(T t) throws Exception;
}
