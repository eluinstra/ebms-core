package nl.clockwork.ebms.cpa;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CPADAOConfig
{
	@Autowired
	DataSource dataSource;

	@Bean
	public URLMappingDAO urlMappingDAO() throws Exception
	{
		val factoryBean = new MapperFactoryBean<URLMappingMapper>(URLMappingMapper.class);
		factoryBean.setSqlSessionFactory(sqlSessionFactory());
		return new URLMappingDAOImpl(factoryBean.getObject());
	}

	@Bean
	public CertificateMappingDAO certificateMappingDAO() throws Exception
	{
		val factoryBean = new MapperFactoryBean<CertificateMappingMapper>(CertificateMappingMapper.class);
		factoryBean.setSqlSessionFactory(sqlSessionFactory());
		return new CertificateMappingDAOImpl(factoryBean.getObject());
	}

	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception
	{
		val factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		SqlSessionFactory result = factoryBean.getObject();
		result.getConfiguration().addMapper(URLMappingMapper.class);
		result.getConfiguration().addMapper(CertificateMappingMapper.class);
		return result;
	}
}
