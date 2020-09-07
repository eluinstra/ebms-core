package nl.clockwork.ebms.cache;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import nl.clockwork.ebms.cache.CacheConfig.CacheType;

public class SomeCacheType implements Condition
{
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
	{
		return context.getEnvironment().getProperty("cache.type",CacheType.class,CacheType.DEFAULT) != CacheType.NONE;
	}
}
