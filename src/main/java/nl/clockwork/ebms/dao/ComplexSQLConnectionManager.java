package nl.clockwork.ebms.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Stack;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ComplexSQLConnectionManager implements ConnectionManager
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
		public Stack<Savepoint> transactionStack = new Stack<Savepoint>();

		public static Connection getConnection(DataSource dataSource) throws DAOException
		{
			return getConnection(dataSource,false);
		}

		public static Connection getConnection(DataSource dataSource, boolean startTransaction) throws DAOException
		{
			try
			{
				if (threadLocal.get().connection == null)
				{
					threadLocal.get().connection = dataSource.getConnection();
					threadLocal.get().connection.setAutoCommit(true);
				}
				if (startTransaction)
				{
					threadLocal.get().connection.setAutoCommit(false);
					Savepoint savepoint = threadLocal.get().connection.setSavepoint();
					threadLocal.get().transactionStack.push(savepoint);
				}
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
				if (force || threadLocal.get().transactionStack.size() == 1)
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
				threadLocal.get().connection.rollback(threadLocal.get().transactionStack.peek());
			}
			catch (SQLException e)
			{
				logger.warn("",e);
			}
		}

		public static void close()
		{
			close(false);
		}
		
		public static void close(boolean endTransaction)
		{
			try
			{
				if (threadLocal.get().connection != null)
					if ((!endTransaction && threadLocal.get().transactionStack.size() == 0) || (endTransaction && threadLocal.get().transactionStack.size() == 1))
						threadLocal.get().connection.close();
			}
			catch (SQLException e)
			{
				logger.warn("",e);
			}
			finally
			{
				if (endTransaction)
				{
					Savepoint savepoint = threadLocal.get().transactionStack.pop();
					if (threadLocal.get().transactionStack.size() > 0)
						try
						{
							threadLocal.get().connection.releaseSavepoint(savepoint);
						}
						catch (SQLException e)
						{
							logger.warn("",e);
						}
				}
				if (threadLocal.get().transactionStack.size() == 0)
					threadLocal.get().connection = null;
					//threadLocal.remove();
			}
		}

	}

	public transient Log logger = LogFactory.getLog(getClass());
	public DataSource dataSource;

	public ComplexSQLConnectionManager(DataSource dataSource)
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
		return SQLConnectionHolder.getConnection(dataSource,startTransaction);
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
		SQLConnectionHolder.close(endTransaction);
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