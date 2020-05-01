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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DAOFactoryTest
{
	EbMSDAOFactory daoFactory;
	ComboPooledDataSource ds;
	EbMSDAO dao;
	
	@BeforeEach
	public void init()
	{
		ds = new ComboPooledDataSource();
		val tt = new TransactionTemplate(new DataSourceTransactionManager(ds));
		val jt = new JdbcTemplate(ds);
		daoFactory = new EbMSDAOFactory(ds,tt,jt);
	}

	@Test
	public void testSingleton()
	{
		assertTrue(daoFactory.isSingleton());
	}
	
	@Test
	public void testHsql() throws Exception
	{
		ds.setDriverClass("org.hsqldb.jdbcDriver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(nl.clockwork.ebms.dao.hsqldb.EbMSDAOImpl.class, dao.getClass());	
	}
	
	
	@Test
	public void testMysql() throws Exception
	{
		ds.setDriverClass("com.mysql.jdbc.Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(nl.clockwork.ebms.dao.mysql.EbMSDAOImpl.class, dao.getClass());
		
		ds.setDriverClass("org.mariadb.jdbc.Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(nl.clockwork.ebms.dao.mysql.EbMSDAOImpl.class, dao.getClass());
	}
	
	@Test
	public void testUnknown() throws Exception
	{
		ds.setDriverClass("none");
		assertThrows(RuntimeException.class,()->daoFactory.getObject());
	}

	@Test
	public void testPostgresql() throws Exception
	{
		ds.setDriverClass("org.postgresql.Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(nl.clockwork.ebms.dao.postgresql.EbMSDAOImpl.class, dao.getClass());
	}

	
	@Test
	public void testOracle() throws Exception
	{
		ds.setDriverClass("oracle.jdbc.OracleDriver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(nl.clockwork.ebms.dao.oracle.EbMSDAOImpl.class, dao.getClass());
	}

	@Test
	public void testMssql() throws Exception
	{
		ds.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(nl.clockwork.ebms.dao.mssql.EbMSDAOImpl.class, dao.getClass());
	}
	
	@Test
	public void testDB2() throws Exception
	{
		ds.setDriverClass("com.ibm.db2.jcc.DB2Driver");
		dao = daoFactory.getObject();
		assertNotNull(dao);
		assertEquals(nl.clockwork.ebms.dao.db2.EbMSDAOImpl.class, dao.getClass());
	}
	
}
