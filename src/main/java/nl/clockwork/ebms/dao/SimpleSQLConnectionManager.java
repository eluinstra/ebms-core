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
package nl.clockwork.ebms.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleSQLConnectionManager implements ConnectionManager
{
	private static class SQLConnectionHolder
	{
		private static final Log logger = LogFactory.getLog(SQLConnectionHolder.class);
		private static final ThreadLocal<SQLConnectionHolder> threadLocal =
			new ThreadLocal<SQLConnectionHolder>()
			{
				protected SQLConnectionHolder initialValue()
				{
					return new SQLConnectionHolder();
				};
			};
		
		public Connection connection;
		public int transactionCount;

		public static Connection getConnection(DataSource dataSource) throws DAOException
		{
			try
			{
				if (threadLocal.get().connection == null)
				{
					threadLocal.get().connection = dataSource.getConnection();
					threadLocal.get().connection.setAutoCommit(false);
				}
				threadLocal.get().transactionCount++;
				return threadLocal.get().connection;
			}
			catch (SQLException e)
			{
				throw new DAOException(e);
			}
		}

		public static void commit() throws DAOException
		{
			commit(false);
		}

		public static void commit(boolean force) throws DAOException
		{
			try
			{
				if (force || threadLocal.get().transactionCount == 1)
					threadLocal.get().connection.commit();
			}
			catch (SQLException e)
			{
				throw new DAOException(e);
			}
		}

		public static void rollback()
		{
			try
			{
				threadLocal.get().connection.rollback();
			}
			catch (SQLException e)
			{
				logger.warn("",e);
			}
		}

		public static void close()
		{
			try
			{
				if (threadLocal.get().connection != null)
					if (threadLocal.get().transactionCount == 1)
					{
						threadLocal.get().connection.commit();
						threadLocal.get().connection.close();
					}
			}
			catch (SQLException e)
			{
				logger.warn("",e);
			}
			finally
			{
				threadLocal.get().transactionCount--;
				if (threadLocal.get().transactionCount == 0)
					threadLocal.get().connection = null;
					//threadLocal.remove();
			}
		}

	}

	protected transient Log logger = LogFactory.getLog(getClass());
	protected DataSource dataSource;

	public SimpleSQLConnectionManager(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	@Override
	public Connection getConnection() throws DAOException
	{
		return SQLConnectionHolder.getConnection(dataSource);
	}

	@Override
	public Connection getConnection(boolean startTransaction) throws DAOException
	{
		return SQLConnectionHolder.getConnection(dataSource);
	}

	@Override
	public void commit() throws DAOException
	{
		SQLConnectionHolder.commit();
	}

	@Override
	public void rollback()
	{
		SQLConnectionHolder.rollback();
	}

	@Override
	public void close()
	{
		SQLConnectionHolder.close();
	}
	
	@Override
	public void close(boolean endTransaction)
	{
		SQLConnectionHolder.close();
	}

	@Override
	public void close(Statement ps)
	{
		try
		{
			if (ps != null)
				ps.close();
		}
		catch (SQLException e)
		{
			logger.warn("",e);
		}
	}

}
