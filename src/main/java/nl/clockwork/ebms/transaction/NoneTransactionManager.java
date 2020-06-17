/**
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

public class NoneTransactionManager implements PlatformTransactionManager
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
