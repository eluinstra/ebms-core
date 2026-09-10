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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 * F7: hard, connector-level request body size cap applied to every endpoint on the management API. Jetty 12's {@code HttpConfiguration} only caps request
 * <em>headers</em> (via {@code setRequestHeaderSize}), not the body, so without this filter an oversized or chunked body would be read in full by the
 * downstream servlet. A check on {@code Content-Length} alone is bypassable: a client can omit it (chunked transfer) or send a value smaller than what it
 * actually streams, both of which report {@code -1}/{under-declared} and skip any length-only cap. This filter therefore does two things: (1) fast-rejects
 * requests whose declared {@code Content-Length} already exceeds the limit with a 413, and (2) wraps the request input stream/reader with a size-counting
 * filter that aborts the read with an {@link IOException} the moment the <em>actual</em> number of bytes read exceeds the limit — so chunked or lying bodies
 * are cut off before they are fully buffered/parsed.
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaxRequestBodySizeFilter implements Filter
{
	long maxRequestBytes;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		val param = filterConfig.getInitParameter("maxRequestBytes");
		if (StringUtils.isBlank(param))
			throw new ServletException("maxRequestBytes parameter is required");
		val value = Long.parseLong(param);
		if (value <= 0)
			throw new ServletException("maxRequestBytes must be greater than 0");
		maxRequestBytes = value;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		if (!(request instanceof HttpServletRequest httpRequest))
		{
			chain.doFilter(request, response);
			return;
		}
		// Fast path: reject up-front if the declared length already exceeds the limit, so we never
		// even start reading an oversized body.
		if (httpRequest.getContentLengthLong() > maxRequestBytes)
		{
			log.warn(
					"Rejecting request to {} with Content-Length {} exceeding limit {} bytes.",
					httpRequest.getRequestURI(),
					httpRequest.getContentLengthLong(),
					maxRequestBytes);
			val resp = (HttpServletResponse)response;
			resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
			resp.getWriter().write("Request body too large");
			return;
		}
		// Slow path: cap the actual number of bytes the downstream servlet may read, catching
		// chunked (no Content-Length) and under-declared bodies.
		chain.doFilter(new LimitedHttpServletRequest(httpRequest, maxRequestBytes), response);
	}

	private static final class LimitedHttpServletRequest extends HttpServletRequestWrapper
	{
		private final long maxBytes;

		private LimitedHttpServletRequest(HttpServletRequest request, long maxBytes)
		{
			super(request);
			this.maxBytes = maxBytes;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException
		{
			return new LimitedServletInputStream(super.getInputStream(), maxBytes, getRequestURI());
		}

		@Override
		public BufferedReader getReader() throws IOException
		{
			return new LimitedBufferedReader(super.getReader(), maxBytes, getRequestURI());
		}
	}

	private static final class LimitedServletInputStream extends ServletInputStream
	{
		private final ServletInputStream in;
		private final long maxBytes;
		private final String uri;
		private long bytesRead;

		private LimitedServletInputStream(ServletInputStream in, long maxBytes, String uri)
		{
			this.in = in;
			this.maxBytes = maxBytes;
			this.uri = uri;
		}

		@Override
		public boolean isFinished()
		{
			return in.isFinished();
		}

		@Override
		public boolean isReady()
		{
			return in.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener)
		{
			in.setReadListener(readListener);
		}

		@Override
		public int read() throws IOException
		{
			int b = in.read();
			if (b != -1)
				accountFor(1);
			return b;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException
		{
			int n = in.read(buffer, offset, length);
			if (n > 0)
				accountFor(n);
			return n;
		}

		private void accountFor(int n) throws IOException
		{
			bytesRead += n;
			if (bytesRead > maxBytes)
				throw new IOException("Request body to " + uri + " exceeds the limit of " + maxBytes + " bytes");
		}
	}

	private static final class LimitedBufferedReader extends BufferedReader
	{
		private final long maxBytes;
		private final String uri;
		private long bytesRead;

		private LimitedBufferedReader(Reader in, long maxBytes, String uri)
		{
			super(in);
			this.maxBytes = maxBytes;
			this.uri = uri;
		}

		@Override
		public int read() throws IOException
		{
			int c = super.read();
			if (c != -1)
				accountFor(1);
			return c;
		}

		@Override
		public int read(char[] buffer, int offset, int length) throws IOException
		{
			int n = super.read(buffer, offset, length);
			if (n > 0)
				accountFor(n);
			return n;
		}

		private void accountFor(int n) throws IOException
		{
			bytesRead += n;
			if (bytesRead > maxBytes)
				throw new IOException("Request body to " + uri + " exceeds the limit of " + maxBytes + " characters");
		}
	}
}
