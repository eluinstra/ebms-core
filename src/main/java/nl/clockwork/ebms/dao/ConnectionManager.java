package nl.clockwork.ebms.dao;

import java.sql.Connection;
import java.sql.Statement;

public interface ConnectionManager
{
	Connection getConnection() throws DAOException;
	Connection getConnection(boolean startTransaction) throws DAOException;
	void commit() throws DAOException;
	void rollback();
	void close();
	void close(boolean endTransaction);
	void close(Statement ps);
}
