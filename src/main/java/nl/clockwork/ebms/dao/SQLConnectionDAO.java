package nl.clockwork.ebms.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SQLConnectionDAO
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected DataSource dataSource;

	public SQLConnectionDAO(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	protected Connection getConnection() throws DAOException
	{
		return SQLConnectionManager.getConnection(dataSource);
	}

	protected Connection getConnection(boolean startTransaction) throws DAOException
	{
		return SQLConnectionManager.getConnection(dataSource,startTransaction);
	}

	protected void commit(Connection connection) throws DAOException
	{
		SQLConnectionManager.commit();
	}

	protected void rollback(Connection connection)
	{
		SQLConnectionManager.rollback();
	}

	protected void close(Connection connection) throws DAOException
	{
		SQLConnectionManager.close();
	}
	
	protected void close(Connection connection, boolean endTransaction)
	{
		SQLConnectionManager.close(endTransaction);
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
