package nl.clockwork.ebms.server.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.common.util.concurrent.RateLimiter;

import lombok.val;

public class RateLimiterFilter implements Filter
{
	RateLimiter rateLimiter;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		val permitsPerSecond = Double.parseDouble(filterConfig.getInitParameter("permitsPerSecond"));
		rateLimiter = RateLimiter.create(permitsPerSecond);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		rateLimiter.acquire();
		chain.doFilter(request,response);
}
}
