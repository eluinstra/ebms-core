package nl.clockwork.ebms.dao;

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public abstract class AbstractDAOFactory<T> implements FactoryBean<T>
{
	protected DataSource dataSource;

	@Override
	public T getObject() throws Exception
	{
		if ("org.hsqldb.jdbcDriver".equals(((ComboPooledDataSource)dataSource).getDriverClass()))
			return createHsqldbDAO();
		else if ("com.mysql.jdbc.Driver".equals(((ComboPooledDataSource)dataSource).getDriverClass()))
			return createMysqlDAO();
		else if ("org.postgresql.Driver".equals(((ComboPooledDataSource)dataSource).getDriverClass()))
			return createPostgresDAO();
		else if ("oracle.jdbc.OracleDriver".equals(((ComboPooledDataSource)dataSource).getDriverClass()))
			return createOracleDAO();
		else if ("net.sourceforge.jtds.jdbc.Driver".equals(((ComboPooledDataSource)dataSource).getDriverClass()))
			return createMssqlDAO();
		else if ("com.microsoft.sqlserver.jdbc.SQLServerDriver".equals(((ComboPooledDataSource)dataSource).getDriverClass()))
			return createMssqlDAO();
		return null;
	}

	@Override
	public abstract Class<T> getObjectType();

	@Override
	public boolean isSingleton()
	{
		return true;
	}

	public abstract T createHsqldbDAO();

	public abstract T createMysqlDAO();

	public abstract T createPostgresDAO();

	public abstract T createOracleDAO();

	public abstract T createMssqlDAO();

	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}
	
	public static abstract class DefaultDAOFactory<U> extends AbstractDAOFactory<U>
	{
		@Override
		public U createHsqldbDAO()
		{
			throw new RuntimeException("HSQLDB not supported!");
		}

		@Override
		public U createMysqlDAO()
		{
			throw new RuntimeException("MySQL not supported!");
		}

		@Override
		public U createPostgresDAO()
		{
			throw new RuntimeException("Postgres not supported!");
		}

		@Override
		public U createOracleDAO()
		{
			throw new RuntimeException("Oracle not supported!");
		}

		@Override
		public U createMssqlDAO()
		{
			throw new RuntimeException("MSSQL not supported!");
		}
	}

}
