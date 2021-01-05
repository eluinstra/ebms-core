package nl.clockwork.ebms.security.azure;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import nl.clockwork.ebms.security.KeyStoresType;

public class AzureKeyStoreConfig implements Condition
{
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
	{
		return context.getEnvironment().getProperty("keystores.type",KeyStoresType.class,KeyStoresType.DEFAULT) == KeyStoresType.AZURE;
	}
}