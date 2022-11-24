/*
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

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static nl.clockwork.ebms.Predicates.contains;

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

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public abstract class AbstractDAOFactory<T> implements FactoryBean<T>
{
	@NonNull
	DataSource dataSource;

	@Override
	public T getObject()
	{
		return createDAO(dataSource);
	}

	private T createDAO(DataSource dataSource)
	{
		val driverClassName = getDriverClassName(dataSource) == null ? "db2" : getDriverClassName(dataSource);
		return Match(driverClassName).of(
				Case($(contains("db2")),o -> createDB2DAO()),
				Case($(contains("h2")),o -> createH2DAO()),
				Case($(contains("hsqldb")),o -> createHSQLDBDAO()),
				Case($(contains("mariadb")),o -> createMariaDBDAO()),
				Case($(contains("oracle")),o -> createOracleDAO()),
				Case($(contains("postgresql")),o -> createPostgreSQLDAO()),
				Case($(contains("sqlserver")),o -> createMSSQLDAO()),
				Case($(),o -> {
					throw new IllegalStateException("Jdbc url " + driverClassName + " not recognized!");
				}));
	}

	public static String getDriverClassName(DataSource dataSource)
	{
		if (dataSource instanceof HikariDataSource)
			return ((HikariDataSource)dataSource).getDriverClassName();
		else if (dataSource  instanceof PoolingDataSource)
			return ((PoolingDataSource)dataSource).getClassName();
		else
			return ((AtomikosDataSourceBean)dataSource).getXaDataSourceClassName();
	}

	@Override
	public boolean isSingleton()
	{
		return true;
	}

	public abstract T createDB2DAO();

	public abstract T createH2DAO();

	public abstract T createHSQLDBDAO();

	public abstract T createMSSQLDAO();

	public abstract T createMariaDBDAO();

	public abstract T createOracleDAO();

	public abstract T createPostgreSQLDAO();

	public abstract static class DefaultDAOFactory<U> extends AbstractDAOFactory<U>
	{
		protected DefaultDAOFactory(@NonNull DataSource dataSource)
		{
			super(dataSource);
		}

		@Override
		public U createDB2DAO()
		{
			throw new IllegalStateException("DB2 not supported!");
		}

		@Override
		public U createH2DAO()
		{
			throw new IllegalStateException("H2 not supported!");
		}

		@Override
		public U createHSQLDBDAO()
		{
			throw new IllegalStateException("HSQLDB not supported!");
		}

		@Override
		public U createMSSQLDAO()
		{
			throw new IllegalStateException("MSSQL not supported!");
		}

		@Override
		public U createMariaDBDAO()
		{
			throw new IllegalStateException("MariaDB not supported!");
		}

		@Override
		public U createOracleDAO()
		{
			throw new IllegalStateException("Oracle not supported!");
		}

		@Override
		public U createPostgreSQLDAO()
		{
			throw new IllegalStateException("Postgres not supported!");
		}
	}

}
