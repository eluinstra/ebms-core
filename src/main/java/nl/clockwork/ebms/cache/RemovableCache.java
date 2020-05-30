package nl.clockwork.ebms.cache;

public interface RemovableCache
{
	void remove(String key);
	void removeAll();
}
