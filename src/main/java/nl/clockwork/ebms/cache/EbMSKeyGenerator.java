package nl.clockwork.ebms.cache;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.interceptor.KeyGenerator;

public class EbMSKeyGenerator implements KeyGenerator
{
	@Override
	public Object generate(Object target, Method method, Object...params)
	{
		return method.getName() + "[" + StringUtils.join(params,",") + "]";
	}
}
