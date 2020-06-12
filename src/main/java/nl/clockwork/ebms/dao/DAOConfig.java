package nl.clockwork.ebms.dao;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.event.listener.EbMSMessageEventDAO;
import nl.clockwork.ebms.event.processor.EbMSEventDAO;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class DAOConfig
{
	@Autowired
	DataSource dataSource;

	@Bean
	public EbMSDAO ebMSDAO() throws Exception
	{
		val transactionManager = new DataSourceTransactionManager(dataSource);
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new EbMSDAOFactory(dataSource,transactionTemplate,jdbcTemplate).getObject();
	}

	@Bean
	public EbMSMessageEventDAO ebMSMessageEventDAO() throws Exception
	{
		val transactionManager = new DataSourceTransactionManager(dataSource);
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return (EbMSMessageEventDAO)new EbMSDAOFactory(dataSource,transactionTemplate,jdbcTemplate).getObject();
	}

	@Bean
	public EbMSEventDAO ebMSEventDAO() throws Exception
	{
		val transactionManager = new DataSourceTransactionManager(dataSource);
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return (EbMSEventDAO)new EbMSDAOFactory(dataSource,transactionTemplate,jdbcTemplate).getObject();
	}
}
