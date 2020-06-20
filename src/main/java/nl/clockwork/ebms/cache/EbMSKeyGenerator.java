package nl.clockwork.ebms.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

public class EbMSKeyGenerator implements KeyGenerator
{
	@Override
	public Object generate(Object target, Method method, Object...params)
	{
		return method.getName() + SimpleKeyGenerator.generateKey(params).hashCode();
	}
}