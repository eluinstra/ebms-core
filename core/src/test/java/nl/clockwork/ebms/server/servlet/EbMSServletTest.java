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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import lombok.val;
import nl.clockwork.ebms.server.EbMSHttpHandler;
import nl.clockwork.ebms.server.processor.EbMSProcessorException;
import org.junit.jupiter.api.Test;

class EbMSServletTest
{
	@Test
	void serviceRejectsNonHttpRequests() throws Exception
	{
		val servlet = new EbMSServlet();
		setHttpHandler(servlet, mock(EbMSHttpHandler.class));

		val request = mock(ServletRequest.class);
		val response = mock(ServletResponse.class);

		val exception = assertThrows(ServletException.class, () -> servlet.service(request, response));
		assertEquals("HTTP request/response required", exception.getMessage());
	}

	@Test
	void serviceDelegatesToHttpHandler() throws Exception
	{
		val servlet = new EbMSServlet();
		val httpHandler = mock(EbMSHttpHandler.class);
		setHttpHandler(servlet, httpHandler);

		val request = mock(HttpServletRequest.class);
		val response = mock(HttpServletResponse.class);

		servlet.service(request, response);

		verify(httpHandler).handle(request, response);
	}

	@Test
	void serviceWrapsEbMSProcessorException() throws Exception
	{
		val servlet = new EbMSServlet();
		val httpHandler = mock(EbMSHttpHandler.class);
		setHttpHandler(servlet, httpHandler);

		val request = mock(HttpServletRequest.class);
		val response = mock(HttpServletResponse.class);
		doThrow(new EbMSProcessorException("test")).when(httpHandler).handle(request, response);

		val exception = assertThrows(ServletException.class, () -> servlet.service(request, response));
		assertInstanceOf(EbMSProcessorException.class, exception.getCause());
	}

	private void setHttpHandler(EbMSServlet servlet, EbMSHttpHandler httpHandler) throws NoSuchFieldException, IllegalAccessException
	{
		Field field = EbMSServlet.class.getDeclaredField("httpHandler");
		field.setAccessible(true);
		field.set(servlet, httpHandler);
	}
}
