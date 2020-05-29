package nl.clockwork.ebms.cache;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ignite.IgniteCache;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class IgniteMethodCacheInterceptor implements MethodInterceptor
{
	@NonNull
	IgniteCache<String,Object> cache;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable
	{
		val targetName = invocation.getThis().getClass().getSimpleName();
		val methodName = invocation.getMethod().getName();
		val arguments = invocation.getArguments();
		val key = getKey(targetName,methodName,arguments);
		var o = cache.get(key);
		if (o == null)
		{
			o = invocation.proceed();
			cache.put(key,o);
		}
		return o;
	}

	public static String getKey(String targetName, String methodName, Object...arguments)
	{
		val sb = new StringBuffer();
		sb.append(targetName).append(".").append(methodName);
		sb.append(Stream.of(arguments).map(a -> String.valueOf(a)).collect(Collectors.joining(",","(",")")));
		return sb.toString();
	}
}
