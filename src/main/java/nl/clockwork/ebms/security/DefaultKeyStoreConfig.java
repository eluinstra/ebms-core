package nl.clockwork.ebms.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class DefaultKeyStoreConfig implements Condition
{
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
	{
		return context.getEnvironment().getProperty("keystores.type",String.class,"") == "";
	}
}