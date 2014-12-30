package nl.clockwork.ebms.client;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.FactoryBean;

public class EbMSProxyFactory extends EbMSProxy implements FactoryBean<EbMSProxy>
{
	@Override
	public EbMSProxy getObject() throws Exception
	{
		if (StringUtils.isNotBlank(getHost()))
			return new EbMSProxy(getHost(),getPort(),getUsername(),getPassword(),getNonProxyHosts());
		else
			return null;
	}

	@Override
	public Class<?> getObjectType()
	{
		return EbMSProxy.class;
	}

	@Override
	public boolean isSingleton()
	{
		return true;
	}

}
