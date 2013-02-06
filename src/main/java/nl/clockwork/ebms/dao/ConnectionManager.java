package nl.clockwork.ebms.dao;

import java.sql.Connection;

public class ConnectionManager
{
	private static final ThreadLocal<Connection> connection =  new ThreadLocal<Connection>();
	
	public static void set(Connection connection)
	{
		ConnectionManager.connection.set(connection);
	}
	
	public static void unset()
	{
		connection.remove();
	}
	
	public static Connection get()
	{
		return connection.get();
	}

}
