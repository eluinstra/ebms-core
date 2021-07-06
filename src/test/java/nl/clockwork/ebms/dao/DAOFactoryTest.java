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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyVetoException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.atomikos.jdbc.internal.AtomikosSQLException;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DAOFactoryTest
{
	EbMSDAOFactory daoFactory;
	HikariDataSource ds;
	EbMSDAO dao;
	
	@BeforeEach
	public void init() throws AtomikosSQLException, PropertyVetoException
	{
		ds = new HikariDataSource();
		val transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(ds));
		val jdbcTemplate = new JdbcTemplate(ds);
		daoFactory = new EbMSDAOFactory(ds,transactionTemplate,jdbcTemplate);
	}

	@Test
	public void testSingleton()
	{
		assertTrue(daoFactory.isSingleton());
	}
	
	@Test
	public void testDB2() throws Exception
	{
		ds.setDriverClassName("com.ibm.db2.jcc.DB2Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(DB2EbMSDAO.class,dao.getClass());
	}
	
	@Test
	public void testH2() throws Exception
	{
		ds.setDriverClassName("org.h2.Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(H2EbMSDAO.class,dao.getClass());	
	}
	
	@Test
	public void testHsql() throws Exception
	{
		ds.setDriverClassName("org.hsqldb.jdbcDriver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(HSQLDBEbMSDAO.class,dao.getClass());	
	}
	
	@Test
	public void testMariadb() throws Exception
	{
		ds.setDriverClassName("org.mariadb.jdbc.Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(MySQLEbMSDAO.class,dao.getClass());
	}
	
	@Test
	public void testMssql() throws Exception
	{
		ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(MSSQLEbMSDAO.class,dao.getClass());
	}
	
	@Test
	public void testMysql() throws Exception
	{
		ds.setDriverClassName("com.mysql.jdbc.Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(MySQLEbMSDAO.class,dao.getClass());
	}
	
	@Test
	public void testOracle() throws Exception
	{
		ds.setDriverClassName("oracle.jdbc.OracleDriver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(OracleEbMSDAO.class,dao.getClass());
	}

	@Test
	public void testPostgresql() throws Exception
	{
		ds.setDriverClassName("org.postgresql.Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(PostgreSQLEbMSDAO.class,dao.getClass());
	}
}
