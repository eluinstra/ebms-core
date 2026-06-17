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
package nl.clockwork.ebms.server.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.val;
import nl.clockwork.ebms.server.processing.MessageRouter;
import org.junit.jupiter.api.Test;

class EbMSInputStreamHandlerTest
{
	@Test
	void handleRejectsRequestLargerThanConfiguredLimit()
	{
		val messageProcessor = mock(MessageRouter.class);
		val handler = new TestHandler(messageProcessor, 16, 128, "POST", 17, "\"ebXML\"");

		handler.handle(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

		assertEquals(500, handler.statusCode);
		assertEquals("text/xml", handler.contentType);
		assertTrue(handler.responseBody().contains("Request too large"));
		verifyNoInteractions(messageProcessor);
	}

	private static class TestHandler extends EbMSInputStreamHandler
	{
		final String method;
		final long contentLength;
		final String soapAction;
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		int statusCode;
		String contentType;

		TestHandler(MessageRouter messageProcessor, long maxRequestBytes, int maxLoggedPayloadChars, String method, long contentLength, String soapAction)
		{
			super(messageProcessor, maxRequestBytes, maxLoggedPayloadChars);
			this.method = method;
			this.contentLength = contentLength;
			this.soapAction = soapAction;
		}

		@Override
		public List<String> getRequestHeaderNames()
		{
			return List.of("SOAPAction", "Content-Type");
		}

		@Override
		public List<String> getRequestHeaders(String headerName)
		{
			if ("SOAPAction".equals(headerName))
				return List.of(soapAction);
			if ("Content-Type".equals(headerName))
				return List.of("text/xml; charset=UTF-8");
			return List.of();
		}

		@Override
		public String getRequestHeader(String headerName)
		{
			if ("SOAPAction".equals(headerName))
				return soapAction;
			if ("Content-Type".equals(headerName))
				return "text/xml; charset=UTF-8";
			return null;
		}

		@Override
		public String getRequestMethod()
		{
			return method;
		}

		@Override
		public long getRequestContentLength()
		{
			return contentLength;
		}

		@Override
		public void writeResponseStatus(int statusCode)
		{
			this.statusCode = statusCode;
		}

		@Override
		public void writeResponseHeader(String name, String value)
		{
			if ("Content-Type".equals(name))
				this.contentType = value;
		}

		@Override
		public OutputStream getOutputStream() throws IOException
		{
			return outputStream;
		}

		String responseBody()
		{
			return outputStream.toString(StandardCharsets.UTF_8);
		}
	}
}
