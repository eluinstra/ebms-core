package nl.clockwork.ebms.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SQLDAO
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected DataSource dataSource;

	public SQLDAO(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	protected Connection getConnection() throws DAOException
	{
		return getConnection(false);
	}

	protected Connection getConnection(boolean startTransaction) throws DAOException
	{
		try
		{
			Connection connection = ConnectionManager.get();
			if (connection == null)
			{
				connection = dataSource.getConnection();
				connection.setAutoCommit(true);
			}
			if (startTransaction)
			{
				connection.setAutoCommit(false);
				ConnectionManager.set(connection);
			}
			return connection;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
	}

	protected void commit(Connection connection) throws DAOException
	{
		if (ConnectionManager.commit())
			try
			{
				connection.commit();
				connection.setAutoCommit(true);
			}
			catch (SQLException e)
			{
				throw new DAOException(e);
			}
	}

	protected void rollback(Connection connection)
	{
		try
		{
			connection.rollback();
		}
		catch (SQLException e)
		{
			logger.warn("",e);
		}
	}

	protected void close(Connection connection) throws DAOException
	{
		close(connection,false);
	}
	
	protected void close(Connection connection, boolean endTransaction)
	{
		try
		{
			if (connection != null)
				if ((!endTransaction && ConnectionManager.get() == null)|| (endTransaction && ConnectionManager.close()))
					connection.close();
		}
		catch (SQLException e)
		{
			logger.warn("",e);
		}
		finally
		{
			if (endTransaction)
				ConnectionManager.unset();
		}
	}

	protected void close(Statement ps)
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
