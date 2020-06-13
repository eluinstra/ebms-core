package nl.clockwork.ebms.dao;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAMapper;
import nl.clockwork.ebms.cpa.certificate.CertificateMappingMapper;
import nl.clockwork.ebms.cpa.url.URLMappingMapper;
import nl.clockwork.ebms.event.listener.EbMSMessageEventDAO;
import nl.clockwork.ebms.event.processor.EbMSEventMapper;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class DAOConfig
{
	@Autowired
	DataSource dataSource;

	@Bean
	public EbMSDAO ebMSDAO() throws Exception
	{
		return new EbMSDAOFactory(dataSource,transactionTemplate(),jdbcTemplate()).getObject();
	}

	@Bean
	public EbMSMessageEventDAO ebMSMessageEventDAO() throws Exception
	{
		return (EbMSMessageEventDAO)new EbMSDAOFactory(dataSource,transactionTemplate(),jdbcTemplate()).getObject();
	}

	@Bean
	public DataSourceTransactionManager transactionManager()
	{
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public TransactionTemplate transactionTemplate()
	{
		return new TransactionTemplate(transactionManager());
	}

	@Bean
	public org.springframework.jdbc.core.JdbcTemplate jdbcTemplate()
	{
		return new JdbcTemplate(dataSource);
	}

	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception
	{
		val factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		SqlSessionFactory result = factoryBean.getObject();
		result.getConfiguration().addMapper(CPAMapper.class);
		result.getConfiguration().addMapper(URLMappingMapper.class);
		result.getConfiguration().addMapper(CertificateMappingMapper.class);
		result.getConfiguration().addMapper(EbMSEventMapper.class);
		return result;
	}

}
