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


import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.slf4j.MDC;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RemoteAddressMDCFilter implements Filter
{
	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		// do nothing
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			val ip = getRemoteAddress((HttpServletRequest)request);
			MDC.put("remoteAddress", ip);
			chain.doFilter(request, response);
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
