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
package nl.clockwork.ebms.dao.spring;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import nl.clockwork.ebms.dao.AbstractDAOFactory;
import nl.clockwork.ebms.dao.EbMSDAO;

public class EbMSDAOFactory extends AbstractDAOFactory<EbMSDAO>
{
	protected TransactionTemplate transactionTemplate;
	protected JdbcTemplate jdbcTemplate;

	@Override
	public Class<EbMSDAO> getObjectType()
	{
		return nl.clockwork.ebms.dao.EbMSDAO.class;
	}

	@Override
	public EbMSDAO createHsqldbDAO()
	{
		return new nl.clockwork.ebms.dao.spring.hsqldb.EbMSDAOImpl(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createMysqlDAO()
	{
		return new nl.clockwork.ebms.dao.spring.mysql.EbMSDAOImpl(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createPostgresDAO()
	{
		return new nl.clockwork.ebms.dao.spring.postgresql.EbMSDAOImpl(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createOracleDAO()
	{
		return new nl.clockwork.ebms.dao.spring.oracle.EbMSDAOImpl(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createMssqlDAO()
	{
		return new nl.clockwork.ebms.dao.spring.mssql.EbMSDAOImpl(transactionTemplate,jdbcTemplate);
	}

	public void setTransactionTemplate(TransactionTemplate transactionTemplate)
	{
		this.transactionTemplate = transactionTemplate;
	}
	
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate)
	{
		this.jdbcTemplate = jdbcTemplate;
	}

}
