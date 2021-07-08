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
class EbMSDAOFactory extends AbstractDAOFactory<EbMSDAO>
{
	@NonNull
	TransactionTemplate transactionTemplate; 
	@NonNull
	JdbcTemplate jdbcTemplate;

	public EbMSDAOFactory(DataSource dataSource, @NonNull TransactionTemplate transactionTemplate, @NonNull JdbcTemplate jdbcTemplate)
	{
		super(dataSource);
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Class<EbMSDAO> getObjectType()
	{
		return EbMSDAO.class;
	}

	@Override
	public EbMSDAO createDB2DAO()
	{
		return new DB2EbMSDAO(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createH2DAO()
	{
		return new H2EbMSDAO(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createHSQLDBDAO()
	{
		return new HSQLDBEbMSDAO(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createMSSQLDAO()
	{
		return new MSSQLEbMSDAO(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createMySQLDAO()
	{
		return new MySQLEbMSDAO(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createOracleDAO()
	{
		return new OracleEbMSDAO(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createPostgreSQLDAO()
	{
		return new PostgreSQLEbMSDAO(transactionTemplate,jdbcTemplate);
	}
}
