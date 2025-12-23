package nl.clockwork.ebms.common.transactionmanager;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class DefaultTransactionManagerType implements Condition
{
	@Override
	public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata)
	{
		return context.getEnvironment().getProperty("transactionManager.type", TransactionManagerType.class, TransactionManagerType.DEFAULT)
				== TransactionManagerType.DEFAULT;
	}
}
