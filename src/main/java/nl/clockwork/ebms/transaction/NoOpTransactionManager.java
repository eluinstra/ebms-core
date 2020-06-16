package nl.clockwork.ebms.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

public class NoOpTransactionManager implements PlatformTransactionManager
{
	@Override
	public void rollback(TransactionStatus status) throws TransactionException
	{
	}
	@Override
	public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException
	{
		return new TransactionStatus()
				{
					@Override
					public boolean isNewTransaction()
					{
						return false;
					}
					@Override
					public void setRollbackOnly()
					{
					}
					@Override
					public boolean isRollbackOnly()
					{
						return false;
					}
					@Override
					public boolean isCompleted()
					{
						return true;
					}
					@Override
					public Object createSavepoint() throws TransactionException
					{
						return null;
					}
					@Override
					public void rollbackToSavepoint(Object savepoint) throws TransactionException
					{
					}
					@Override
					public void releaseSavepoint(Object savepoint) throws TransactionException
					{
					}
					@Override
					public boolean hasSavepoint()
					{
						return false;
					}
					@Override
					public void flush()
					{
					}
				};
	}
	@Override
	public void commit(TransactionStatus status) throws TransactionException
	{
	}
}
