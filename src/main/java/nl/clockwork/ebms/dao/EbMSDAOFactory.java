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
