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

import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.validation.ClientCertificateManager;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRateLimiterFilter implements Filter
{
	ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
	double queriesPerSecond;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		this.queriesPerSecond = Double.parseDouble(filterConfig.getInitParameter("queriesPerSecond"));
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		val subject = Optional.ofNullable(ClientCertificateManager.getCertificate()).map(c -> c.getSubjectDN().toString()).orElse("");
		rateLimiters.computeIfAbsent(subject, s -> RateLimiter.create(queriesPerSecond));
		rateLimiters.get(subject).acquire();
		chain.doFilter(request, response);
	}
}
