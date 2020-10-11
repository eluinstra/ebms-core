package nl.clockwork.ebms.server.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.MDC;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RemoteAddressMDCFilter implements Filter
{
	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			val ip = getRemoteAddress((HttpServletRequest)request);
			MDC.put("remoteAddress",ip);
			chain.doFilter(request,response);
		}
		finally
		{
			MDC.remove("remoteAddress");
		}
	}

	private String getRemoteAddress(HttpServletRequest request)
	{
		val ip = request.getHeader("X-Forward-For");
		return ip == null ? request.getRemoteAddr() : ip;
	}
}
