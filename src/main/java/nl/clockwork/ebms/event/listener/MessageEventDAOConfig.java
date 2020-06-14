package nl.clockwork.ebms.event.listener;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageEventDAOConfig
{
	@Autowired
	SqlSessionFactory sqlSessionFactory;

	@Bean
	public EbMSMessageEventDAO ebMSMessageEventDAO() throws Exception
	{
		val factoryBean = new MapperFactoryBean<EbMSMessageEventMapper>(EbMSMessageEventMapper.class);
		factoryBean.setSqlSessionFactory(sqlSessionFactory);
		return new EbMSMessageEventDAOImpl(factoryBean.getObject());
	}
}
