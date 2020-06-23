package nl.clockwork.ebms.event.processor;

import org.springframework.transaction.annotation.Transactional;

import nl.clockwork.ebms.Action;

public class TransactionTemplate
{
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public void executeTransaction(Action action)
	{
		action.run();
	}
}
