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

public class EbMSDAOFactory extends AbstractDAOFactory<EbMSDAO>
{
	private ConnectionManager connectionManager;

	@Override
	public Class<EbMSDAO> getObjectType()
	{
		return nl.clockwork.ebms.dao.EbMSDAO.class;
	}

	@Override
	public EbMSDAO createHsqldbDAO()
	{
		return new nl.clockwork.ebms.dao.hsqldb.EbMSDAOImpl(connectionManager);
	}

	@Override
	public EbMSDAO createMysqlDAO()
	{
		return new nl.clockwork.ebms.dao.mysql.EbMSDAOImpl(connectionManager);
	}

	@Override
	public EbMSDAO createPostgresDAO()
	{
		return new nl.clockwork.ebms.dao.postgresql.EbMSDAOImpl(connectionManager);
	}

	@Override
	public EbMSDAO createOracleDAO()
	{
		return new nl.clockwork.ebms.dao.oracle.EbMSDAOImpl(connectionManager);
	}

	@Override
	public EbMSDAO createMssqlDAO()
	{
		return new nl.clockwork.ebms.dao.mssql.EbMSDAOImpl(connectionManager);
	}

	public void setConnectionManager(ConnectionManager connectionManager)
	{
		this.connectionManager = connectionManager;
	}
}
