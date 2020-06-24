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

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static nl.clockwork.ebms.Predicates.startsWith;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;

import com.atomikos.jdbc.internal.AtomikosSQLException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.datasource.DataSourceConfig;
import nl.clockwork.ebms.transaction.TransactionManagerConfig;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.TransactionManagerType;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public abstract class AbstractDAOFactory<T> implements FactoryBean<T>
{
	@NonNull
	TransactionManagerConfig.TransactionManagerType transactionManagerType;
	@NonNull
	DataSource dataSource;

	@Override
	public T getObject() throws Exception
	{
		return createDAO(dataSource);
	}

	private T createDAO(DataSource dataSource) throws AtomikosSQLException, PropertyVetoException
	{
		String jdbcUrl = DataSourceConfig.getJdbcUrl(dataSource);
		return Match(jdbcUrl).of(
				Case($(startsWith("jdbc:db2:")),o -> createDB2DAO()),
				Case($(startsWith("jdbc:hsqldb:")),o -> createHSqlDbDAO()),
				Case($(startsWith("jdbc:mysql:")),o -> createMySqlDAO()),
				Case($(startsWith("jdbc:oracle:")),o -> createOracleDAO()),
				Case($(startsWith("jdbc:postgresql:")),o -> createPostgresDAO()),
				Case($(startsWith("jdbc:sqlserver:")),o -> createMsSqlDAO()),
				Case($(),o -> {
					throw new RuntimeException("Jdbc url " + jdbcUrl + " not recognized!");
				}));
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
