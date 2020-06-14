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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSDAOFactory extends AbstractDAOFactory<EbMSDAO>
{
	@NonNull
	TransactionTemplate transactionTemplate;
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	EbMSMessageMapper ebMSMessageMapper;

	public EbMSDAOFactory(DataSource dataSource, @NonNull TransactionTemplate transactionTemplate, @NonNull JdbcTemplate jdbcTemplate, @NonNull EbMSMessageMapper ebMSMessageMapper)
	{
		super(dataSource);
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
		this.ebMSMessageMapper = ebMSMessageMapper;
	}

	@Override
	public Class<EbMSDAO> getObjectType()
	{
		return EbMSDAO.class;
	}

	@Override
	public EbMSDAO createHSqlDbDAO()
	{
		return new HSQLDBEbMSDAO(transactionTemplate,jdbcTemplate,ebMSMessageMapper);
	}

	@Override
	public EbMSDAO createMySqlDAO()
	{
		return new MySQLEbMSDAO(transactionTemplate,jdbcTemplate,ebMSMessageMapper);
	}

	@Override
	public EbMSDAO createPostgresDAO()
	{
		return new PostgreSQLEbMSDAO(transactionTemplate,jdbcTemplate,ebMSMessageMapper);
	}

	@Override
	public EbMSDAO createOracleDAO()
	{
		return new OracleEbMSDAO(transactionTemplate,jdbcTemplate,ebMSMessageMapper);
	}

	@Override
	public EbMSDAO createMsSqlDAO()
	{
		return new MSSQLEbMSDAO(transactionTemplate,jdbcTemplate,ebMSMessageMapper);
	}

	@Override
	public EbMSDAO createDB2DAO()
	{
		return new DB2EbMSDAO(transactionTemplate,jdbcTemplate,ebMSMessageMapper);
	}
}
