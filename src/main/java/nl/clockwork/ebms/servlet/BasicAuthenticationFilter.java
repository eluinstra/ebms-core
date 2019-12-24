package nl.clockwork.ebms.servlet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class BasicAuthenticationFilter implements Filter
{
	private String realm;
	private Map<String,String> users;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try
		{
			realm = filterConfig.getInitParameter("realm");
			File realmFile = new File(filterConfig.getInitParameter("realmFile"));
			List<String> lines = FileUtils.readLines(realmFile,Charset.defaultCharset());
			users = lines.stream()
					.map(s -> StringUtils.split(s,","))
					.filter(a -> a.length == 2 && "user".equals(a[1]))
					.map(a -> StringUtils.split(a[0],":"))
					.filter(u -> u.length == 2)
					.collect(Collectors.toMap(u -> u[0],u -> u[1]));
		}
		catch (IOException e)
		{
			throw new ServletException(e);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		String authorization = ((HttpServletRequest)request).getHeader("Authorization");
		if (validate(users,authorization))
			chain.doFilter(request,response);
		else
		{
			((HttpServletResponse)response).setHeader("WWW-Authenticate","Basic realm=\"" + realm + "\"");
			((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED,"Unauthorized");
		}
	}

	private boolean validate(Map<String,String> users, String authorization)
	{
		if (authorization != null && authorization.toLowerCase().startsWith("basic"))
		{
			authorization = authorization.substring("basic".length()).trim();
			authorization = new String(Base64.getDecoder().decode(authorization));
			String[] credenitals = StringUtils.split(authorization,":");
			if (credenitals.length == 2)
				return credenitals[1].equals(users.get(credenitals[0]));
		}
		return false;
	}

	@Override
	public void destroy()
	{
	}

}
