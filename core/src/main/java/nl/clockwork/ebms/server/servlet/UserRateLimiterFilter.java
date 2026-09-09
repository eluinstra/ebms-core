/*
 * Copyright 2011 - 2026 Clockwork
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.validation.ClientCertificateManager;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRateLimiterFilter implements Filter
{
	// Upper bound on the number of per-client limiters retained in memory. Bounding the
	// cache prevents memory exhaustion when the client identifier (the client-certificate
	// subject, which can be client-supplied when running behind a proxy) takes many distinct values.
	private static final int MAX_TRACKED_CLIENTS = 10000;
	// Fallback identifier for clients without a client certificate. Using the remote address
	// keeps anonymous callers from all sharing a single limiter bucket.
	private static final String ANONYMOUS_FALLBACK = "anonymous";

	Cache<String, RateLimiter> rateLimiters = CacheBuilder.newBuilder().maximumSize(MAX_TRACKED_CLIENTS).build();
	double queriesPerSecond;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		this.queriesPerSecond = Double.parseDouble(filterConfig.getInitParameter("queriesPerSecond"));
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		val subject = Optional.ofNullable(ClientCertificateManager.getCertificate())
						.map(c -> c.getSubjectX500Principal().toString())
						.filter(s -> !s.isEmpty())
						.orElse(remoteAddress(request));
		rateLimiters.asMap().computeIfAbsent(subject, s -> RateLimiter.create(queriesPerSecond)).acquire();
		chain.doFilter(request, response);
	}

	private String remoteAddress(ServletRequest request)
	{
		if (request instanceof HttpServletRequest http)
			return http.getRemoteAddr();
		return ANONYMOUS_FALLBACK;
	}
}
