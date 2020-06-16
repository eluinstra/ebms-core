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

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.zaxxer.hikari.HikariDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOConfig.TransactionManagerType;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public abstract class AbstractDAOFactory<T> implements FactoryBean<T>
{
	@NonNull
	TransactionManagerType transactionManagerType;
	@NonNull
	DataSource dataSource;

	@Override
	public T getObject() throws Exception
	{
		switch(transactionManagerType)
		{
			case BITRONIX:
			case ATOMIKOS:
				return createXADataSource();
			default:
				return createDefaultDataSource();
		}
	}

	private T createXADataSource()
	{
		val driverClassName = dataSource instanceof PoolingDataSource ? ((PoolingDataSource)dataSource).getClassName() : ((AtomikosDataSourceBean)dataSource).getXaDataSourceClassName();
		switch (driverClassName)
		{
			case "org.hsqldb.jdbc.pool.JDBCXADataSource":
				return createHSqlDbDAO();
			case "com.mysql.jdbc.Driver":
				return createMySqlDAO();
			case "org.mariadb.jdbc.Driver":
				return createMySqlDAO();
			case "org.postgresql.Driver":
				return createPostgresDAO();
			case "oracle.jdbc.OracleDriver":
				return createOracleDAO();
			case "com.microsoft.sqlserver.jdbc.SQLServerDriver":
				return createMsSqlDAO();
			case "com.ibm.db2.jcc.DB2Driver":
				return createDB2DAO();
			default:
				throw new RuntimeException("SQL Driver " + driverClassName + " not recognized!");
		}
	}

	private T createDefaultDataSource()
	{
		switch (((HikariDataSource)dataSource).getDriverClassName())
		{
			case "org.hsqldb.jdbcDriver":
				return createHSqlDbDAO();
			case "com.mysql.jdbc.Driver":
				return createMySqlDAO();
			case "org.mariadb.jdbc.Driver":
				return createMySqlDAO();
			case "org.postgresql.Driver":
				return createPostgresDAO();
			case "oracle.jdbc.OracleDriver":
				return createOracleDAO();
			case "com.microsoft.sqlserver.jdbc.SQLServerDriver":
				return createMsSqlDAO();
			case "com.ibm.db2.jcc.DB2Driver":
				return createDB2DAO();
			default:
				throw new RuntimeException("SQL Driver " + ((HikariDataSource)dataSource).getDriverClassName() + " not recognized!");
		}
	}

	@Override
	public abstract Class<T> getObjectType();

	@Override
	public boolean isSingleton()
	{
		return true;
	}

	public abstract T createHSqlDbDAO();

	public abstract T createMySqlDAO();

	public abstract T createPostgresDAO();

	public abstract T createOracleDAO();

	public abstract T createMsSqlDAO();

	public abstract T createDB2DAO();

	public abstract static class DefaultDAOFactory<U> extends AbstractDAOFactory<U>
	{
		public DefaultDAOFactory(@NonNull TransactionManagerType transactionManagerType, @NonNull DataSource dataSource)
		{
			super(transactionManagerType,dataSource);
		}

		@Override
		public U createHSqlDbDAO()
		{
			throw new RuntimeException("HSQLDB not supported!");
		}

		@Override
		public U createMySqlDAO()
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
		public U createMsSqlDAO()
		{
			throw new RuntimeException("MSSQL not supported!");
		}

		@Override
		public U createDB2DAO()
		{
			throw new RuntimeException("DB2 not supported!");
		}

	}

}
