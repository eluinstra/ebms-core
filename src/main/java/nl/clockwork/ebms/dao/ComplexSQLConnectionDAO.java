package nl.clockwork.ebms.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ComplexSQLConnectionDAO
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected DataSource dataSource;

	public ComplexSQLConnectionDAO(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	protected Connection getConnection() throws DAOException
	{
		return ComplexSQLConnectionManager.getConnection(dataSource);
	}

	protected Connection getConnection(boolean startTransaction) throws DAOException
	{
		return ComplexSQLConnectionManager.getConnection(dataSource,startTransaction);
	}

	protected void commit(Connection connection) throws DAOException
	{
		ComplexSQLConnectionManager.commit();
	}

	protected void rollback(Connection connection)
	{
		ComplexSQLConnectionManager.rollback();
	}

	protected void close(Connection connection) throws DAOException
	{
		ComplexSQLConnectionManager.close();
	}
	
	protected void close(Connection connection, boolean endTransaction)
	{
		ComplexSQLConnectionManager.close(endTransaction);
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
