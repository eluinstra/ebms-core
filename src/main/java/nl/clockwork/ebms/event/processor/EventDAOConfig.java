package nl.clockwork.ebms.event.processor;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventDAOConfig
{
	@Autowired
	SqlSessionFactory sqlSessionFactory;
	@Autowired
	TransactionTemplate transactionTemplate;

	@Bean
	public EbMSEventDAO ebMSEventDAO() throws Exception
	{
		val factoryBean = new MapperFactoryBean<EbMSEventMapper>(EbMSEventMapper.class);
		factoryBean.setSqlSessionFactory(sqlSessionFactory);
		return new EbMSEventDAOImpl(transactionTemplate,factoryBean.getObject());
	}
}
