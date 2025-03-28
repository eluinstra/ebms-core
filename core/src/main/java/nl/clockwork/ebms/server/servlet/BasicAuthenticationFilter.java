/*
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.server.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BasicAuthenticationFilter implements Filter
{
	String realm;
	Map<String, String> users;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try
		{
			realm = filterConfig.getInitParameter("realm");
			val realmFile = new File(filterConfig.getInitParameter("realmFile"));
			val lines = FileUtils.readLines(realmFile, Charset.defaultCharset());
			users = lines.stream()
					.map(s -> StringUtils.split(s, ","))
					.filter(a -> a.length == 2 && "user".equals(a[1]))
					.map(a -> StringUtils.split(a[0], ":"))
					.filter(u -> u.length == 2)
					.collect(Collectors.toMap(u -> u[0], u -> u[1]));
		}
		catch (IOException e)
		{
			throw new ServletException(e);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		val authorization = ((HttpServletRequest)request).getHeader("Authorization");
		if (validate(users, authorization))
			chain.doFilter(request, response);
		else
		{
			((HttpServletResponse)response).setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
			((HttpServletResponse)response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	private boolean validate(Map<String, String> users, String authorization) throws ServletException
	{
		try
		{
			if (authorization != null && authorization.toLowerCase().startsWith("basic"))
			{
				authorization = authorization.substring("basic".length()).trim();
				authorization = new String(Base64.getDecoder().decode(authorization));
				val credenitals = StringUtils.split(authorization, ":");
				if (credenitals.length == 2)
					return validate(users.get(credenitals[0]), credenitals[1]);
			}
			return false;
		}
		catch (NoSuchAlgorithmException | UnsupportedEncodingException e)
		{
			throw new ServletException(e);
		}
	}

	// TODO: support all allowed password encodings
	private boolean validate(String savedPassword, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		if (savedPassword.startsWith("MD5:"))
			return toMD5(password).equals(savedPassword);
		else if (savedPassword.startsWith("OBF:"))
			throw new NoSuchAlgorithmException("OBF");
		else if (savedPassword.startsWith("CRYPT:"))
			throw new NoSuchAlgorithmException("CRYPT");
		else
			return password.equals(savedPassword);
	}

	private String toMD5(String s)
	{
		return "MD5:" + DigestUtils.md5Hex(s);
	}

	@Override
	public void destroy()
	{
		// do nothing
	}

}
