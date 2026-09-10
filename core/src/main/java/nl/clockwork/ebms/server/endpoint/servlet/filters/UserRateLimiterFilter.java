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
package nl.clockwork.ebms.server.endpoint.servlet.filters;

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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.server.security.certificate.ClientCertificateManager;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRateLimiterFilter implements Filter
{
	// F4: bound the number of tracked per-user buckets. The old implementation used an
	// unbounded ConcurrentHashMap keyed by certificate subject, so unique subjects (or a
	// spoofable certificate source, see ClientCertificateManagerFilter) could grow the map
	// without limit. A size-capped LRU cache evicts stale buckets instead of leaking memory.
	private static final int MAX_TRACKED_USERS = 10_000;

	Cache<String, RateLimiter> rateLimiters = CacheBuilder.newBuilder().maximumSize(MAX_TRACKED_USERS).build();
	double queriesPerSecond;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		this.queriesPerSecond = Double.parseDouble(filterConfig.getInitParameter("queriesPerSecond"));
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		val key = rateLimitKey(request);
		RateLimiter rateLimiter;
		try
		{
			// The callable (RateLimiter.create) cannot throw; the checked ExecutionException
			// is only declared by the Guava signature and can only occur if that callable
			// fails, in which case we fall back to a fresh limiter.
			rateLimiter = rateLimiters.get(key, () -> RateLimiter.create(queriesPerSecond));
		}
		catch (java.util.concurrent.ExecutionException e)
		{
			log.debug("Creating fresh rate limiter for {}", key, e);
			rateLimiter = RateLimiter.create(queriesPerSecond);
		}
		rateLimiter.acquire();
		chain.doFilter(request, response);
	}

	// F4: the per-user bucket key is the client certificate subject when a certificate is
	// present, falling back to the client IP address otherwise. Bucketing unauthenticated
	// requests by IP keeps a single source from exhausting a shared anonymous bucket (or
	// evading the limit), instead of mapping every unauthenticated caller onto one "" bucket.
	private String rateLimitKey(ServletRequest request)
	{
		val subject = Optional.ofNullable(ClientCertificateManager.getCertificate()).map(c -> c.getSubjectX500Principal().toString()).orElse("");
		val source = request instanceof HttpServletRequest httpRequest ? httpRequest.getRemoteAddr() : "";
		return subject.isEmpty() ? "ip:" + source : "cert:" + subject;
	}
}
