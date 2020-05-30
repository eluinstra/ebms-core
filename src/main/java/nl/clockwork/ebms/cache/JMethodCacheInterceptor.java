package nl.clockwork.ebms.cache;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.cache.Cache;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class JMethodCacheInterceptor implements MethodInterceptor, RemovableCache
{
	@NonNull
	Cache<String,Object> cache;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable
	{
		val targetName = invocation.getThis().getClass().getSimpleName();
		val methodName = invocation.getMethod().getName();
		val arguments = invocation.getArguments();
		val key = getKey(targetName,methodName,arguments);
		if (!cache.containsKey(key))
			cache.put(key,invocation.proceed());
		return cache.get(key);
	}

	public static String getKey(String targetName, String methodName, Object...arguments)
	{
		val sb = new StringBuffer();
		sb.append(targetName).append(".").append(methodName);
		sb.append(Stream.of(arguments).map(a -> String.valueOf(a)).collect(Collectors.joining(",","(",")")));
		return sb.toString();
	}

	@Override
	public void remove(String key)
	{
		cache.remove(key);
	}

	@Override
	public void removeAll()
	{
		cache.removeAll();
	}
}
