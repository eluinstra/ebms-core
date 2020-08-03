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

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class EbMSDAOFactory extends AbstractDAOFactory<EbMSDAO>
{
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	SQLQueryFactory queryFactory;

	public EbMSDAOFactory(
			DataSource dataSource,
			@NonNull JdbcTemplate jdbcTemplate,
			@NonNull SQLQueryFactory queryFactory)
	{
		super(dataSource);
		this.jdbcTemplate = jdbcTemplate;
		this.queryFactory = queryFactory;
	}

	@Override
	public Class<EbMSDAO> getObjectType()
	{
		return EbMSDAO.class;
	}

	@Override
	public EbMSDAO createDB2DAO()
	{
		return new DB2EbMSDAO(jdbcTemplate,queryFactory);
	}

	@Override
	public EbMSDAO createH2DAO()
	{
		return new H2EbMSDAO(jdbcTemplate,queryFactory);
	}

	@Override
	public EbMSDAO createHSQLDBDAO()
	{
		return new HSQLDBEbMSDAO(jdbcTemplate,queryFactory);
	}

	@Override
	public EbMSDAO createMSSQLDAO()
	{
		return new MSSQLEbMSDAO(jdbcTemplate,queryFactory);
	}

	@Override
	public EbMSDAO createMySQLDAO()
	{
		return new MySQLEbMSDAO(jdbcTemplate,queryFactory);
	}

	@Override
	public EbMSDAO createOracleDAO()
	{
		return new OracleEbMSDAO(jdbcTemplate,queryFactory);
	}

	@Override
	public EbMSDAO createPostgreSQLDAO()
	{
		return new PostgreSQLEbMSDAO(jdbcTemplate,queryFactory);
	}
}
