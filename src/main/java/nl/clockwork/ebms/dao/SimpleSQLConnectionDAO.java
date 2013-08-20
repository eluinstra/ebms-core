package nl.clockwork.ebms.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleSQLConnectionDAO
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected DataSource dataSource;

	public SimpleSQLConnectionDAO(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	protected Connection getConnection() throws DAOException
	{
		return SimpleSQLConnectionManager.getConnection(dataSource);
	}

	protected Connection getConnection(boolean startTransaction) throws DAOException
	{
		return SimpleSQLConnectionManager.getConnection(dataSource);
	}

	protected void commit(Connection connection) throws DAOException
	{
		SimpleSQLConnectionManager.commit();
	}

	protected void rollback(Connection connection)
	{
		SimpleSQLConnectionManager.rollback();
	}

	protected void close(Connection connection) throws DAOException
	{
		SimpleSQLConnectionManager.close();
	}
	
	protected void close(Connection connection, boolean endTransaction)
	{
		SimpleSQLConnectionManager.close();
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
