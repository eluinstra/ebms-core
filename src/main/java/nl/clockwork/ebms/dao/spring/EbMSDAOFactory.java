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
	public EbMSDAO createHSqlDbDAO()
	{
		return new nl.clockwork.ebms.dao.spring.hsqldb.EbMSDAOImpl(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSDAO createMySqlDAO()
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
	public EbMSDAO createMsSqlDAO()
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
