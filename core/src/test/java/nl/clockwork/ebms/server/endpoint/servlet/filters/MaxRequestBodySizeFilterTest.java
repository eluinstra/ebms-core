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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MaxRequestBodySizeFilterTest
{
	private MaxRequestBodySizeFilter filter;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private FilterChain chain;

	@BeforeEach
	void setUp() throws Exception
	{
		filter = new MaxRequestBodySizeFilter();
		FilterConfig config = mock(FilterConfig.class);
		when(config.getInitParameter("maxRequestBytes")).thenReturn("16");
		filter.init(config);
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		chain = mock(FilterChain.class);
		when(response.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
	}

	@Test
	void rejectsBodyLargerThanDeclaredContentLength() throws Exception
	{
		when(request.getContentLengthLong()).thenReturn(1024L);

		filter.doFilter(request, response, chain);

		verify(response).setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
		verifyNoInteractions(chain);
	}

	@Test
	void capsChunkedBodyThatStreamsMoreThanLimit() throws Exception
	{
		// No declared length (chunked) so the fast path does not reject; the actual read must be capped.
		when(request.getContentLengthLong()).thenReturn(-1L);
		when(request.getInputStream()).thenReturn(servletStream(new byte[64]));
		Mockito.doAnswer(invocation ->
		{
			((HttpServletRequest)invocation.getArgument(0, ServletRequest.class)).getInputStream().readAllBytes();
			return null;
		}).when(chain).doFilter(any(HttpServletRequest.class), any(ServletResponse.class));

		assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isInstanceOf(IOException.class);
	}

	@Test
	void allowsBodyWithinStreamLimit() throws Exception
	{
		when(request.getContentLengthLong()).thenReturn(-1L);
		when(request.getInputStream()).thenReturn(servletStream(new byte[8]));
		Mockito.doAnswer(invocation ->
		{
			((HttpServletRequest)invocation.getArgument(0, ServletRequest.class)).getInputStream().readAllBytes();
			return null;
		}).when(chain).doFilter(any(HttpServletRequest.class), any(ServletResponse.class));

		// Should not throw: 8 bytes < 16 limit.
		filter.doFilter(request, response, chain);
	}

	@Test
	void init_requiresPositiveLimit()
	{
		MaxRequestBodySizeFilter other = new MaxRequestBodySizeFilter();
		FilterConfig config = mock(FilterConfig.class);
		when(config.getInitParameter("maxRequestBytes")).thenReturn("0");
		assertThatThrownBy(() -> other.init(config)).isInstanceOf(jakarta.servlet.ServletException.class);
	}

	@Test
	void init_requiresLimitParameter()
	{
		MaxRequestBodySizeFilter other = new MaxRequestBodySizeFilter();
		FilterConfig config = mock(FilterConfig.class);
		when(config.getInitParameter("maxRequestBytes")).thenReturn(null);
		assertThatThrownBy(() -> other.init(config)).isInstanceOf(jakarta.servlet.ServletException.class);
	}

	private static ServletInputStream servletStream(byte[] data)
	{
		ByteArrayInputStream source = new ByteArrayInputStream(data);
		return new ServletInputStream()
		{
			@Override
			public boolean isFinished()
			{
				return source.available() == 0;
			}

			@Override
			public boolean isReady()
			{
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public int read()
			{
				return source.read();
			}

			@Override
			public int read(byte[] buffer, int offset, int length)
			{
				return source.read(buffer, offset, length);
			}
		};
	}
}
