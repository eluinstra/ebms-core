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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BasicAuthenticationFilter implements Filter
{
	String realm;
	Map<String, String> users;
	ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
	int maxFailedAttempts;
	long lockoutBaseMillis;
	long lockoutMaxMillis;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try
		{
			realm = filterConfig.getInitParameter("realm");
			maxFailedAttempts = parseInt(filterConfig.getInitParameter("maxFailedAttempts"), 5);
			lockoutBaseMillis = parseLong(filterConfig.getInitParameter("lockoutBaseMillis"), 1000L);
			lockoutMaxMillis = parseLong(filterConfig.getInitParameter("lockoutMaxMillis"), 300000L);
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
		val httpRequest = (HttpServletRequest)request;
		val authorization = httpRequest.getHeader("Authorization");
		if (validate(users, authorization, httpRequest.getRemoteAddr()))
			chain.doFilter(request, response);
		else
		{
			((HttpServletResponse)response).setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
			((HttpServletResponse)response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	private boolean validate(Map<String, String> users, String authorization, String remoteAddress)
	{
		val credentials = getCredentials(authorization);
		if (credentials == null)
			return false;
		val username = credentials[0];
		val password = credentials[1];
		val lockKey = remoteAddress + ":" + username;
		if (isLocked(lockKey))
			return false;
		val valid = validate(users.get(username), password);
		if (valid)
			attempts.remove(lockKey);
		else
			onFailedAttempt(lockKey);
		return valid;
	}

	private String[] getCredentials(String authorization)
	{
		if (authorization == null || !authorization.toLowerCase().startsWith("basic"))
			return null;
		try
		{
			authorization = authorization.substring("basic".length()).trim();
			authorization = new String(Base64.getDecoder().decode(authorization), StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
		val credentials = StringUtils.split(authorization, ":");
		if (credentials == null || credentials.length != 2)
			return null;
		return credentials;
	}

	private boolean isLocked(String key)
	{
		val state = attempts.get(key);
		return state != null && state.lockedUntilEpochMillis > System.currentTimeMillis();
	}

	private void onFailedAttempt(String key)
	{
		attempts.compute(key, (k, current) ->
		{
			val state = current == null ? new AttemptState() : current;
			state.failedAttempts++;
			if (state.failedAttempts >= maxFailedAttempts)
			{
				val step = Math.max(0, state.failedAttempts - maxFailedAttempts);
				val lockMillis = Math.min(lockoutMaxMillis, lockoutBaseMillis << Math.min(step, 20));
				state.lockedUntilEpochMillis = System.currentTimeMillis() + lockMillis;
			}
			return state;
		});
	}

	private static int parseInt(String value, int defaultValue)
	{
		if (StringUtils.isBlank(value))
			return defaultValue;
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}

	private static long parseLong(String value, long defaultValue)
	{
		if (StringUtils.isBlank(value))
			return defaultValue;
		try
		{
			return Long.parseLong(value);
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}

	private boolean validate(String savedPassword, String password)
	{
		if (savedPassword == null)
			return false;
		if (savedPassword.startsWith("$2"))
			return BCrypt.checkpw(password, savedPassword);
		if (savedPassword.startsWith("MD5:"))
			return false;
		// Unsupported legacy formats and plaintext are rejected.
		// This avoids continuing insecure password schemes.
		return false;
	}

	@Override
	public void destroy()
	{
		// do nothing
	}

	@FieldDefaults(level = AccessLevel.PRIVATE)
	private static class AttemptState
	{
		int failedAttempts;
		long lockedUntilEpochMillis;
	}

}
