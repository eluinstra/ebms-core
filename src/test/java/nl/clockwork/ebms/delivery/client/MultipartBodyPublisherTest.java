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
package nl.clockwork.ebms.delivery.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import lombok.val;
import nl.clockwork.ebms.EbMSAttachmentFactory;

public class MultipartBodyPublisherTest
{
	@Test
	void testXmlPart() throws IOException
	{
		val contentId = "contentId";
		val value = "<content/>";
		val part = new XmlPart(contentId,value);

		assertEquals(part.getContentId(),contentId);
		val buf = ByteBuffer.allocate(value.length());
		val input = part.open();
		assertEquals(value.length(),input.read(buf));
		assertArrayEquals(value.getBytes(),buf.array());

		assertEquals(UTF_8,part.getCharset());
	}

	@Test
	void testAttachmentPart() throws Exception
	{
		val contentId = "test";
		val filename = "test.txt";
		val value = "Hello world!";
		val contentType = "text/plain";
		val part = new AttachmentPart(contentId,filename,() -> {
			return Channels.newChannel(new ByteArrayInputStream(value.getBytes()));
		},contentType);

		assertEquals(part.getContentId(),contentId);
		val buf = ByteBuffer.allocate(value.length());
		val input = part.open();
		while (input.read(buf) != -1 && buf.hasRemaining())
		{
			// nop
		}
		assertArrayEquals(value.getBytes(),buf.array());

		assertEquals(part.getFilename(),filename);
		assertEquals(part.getContentType(),contentType);
	}

	@Test
	void testMultipartFormDataChannel() throws Exception
	{
		val value = "<content/>";
		val channel = new MultipartChannel("boundary",List.<Part>of(new XmlPart("contentId",value)),UTF_8);
		assertEquals(true,channel.isOpen());
		val content = new StringBuilder();
		try (channel)
		{
			val r = Channels.newReader(channel,UTF_8);
			val buf = new char[1024 * 8];
			int n;
			while ((n = r.read(buf)) != -1)
			{
				content.append(buf,0,n);
			}
		}
		assertEquals(false,channel.isOpen());

		val expect = "--boundary\r\n" + "Content-Type: text/xml; charset=UTF-8\r\n" + "Content-ID: <contentId>\r\n" + "\r\n" + "<content/>\r\n" + "--boundary--\r\n";
		assertEquals(expect,content.toString());
	}

	@Test
	void testMultipartFormDataChannelException() throws Exception
	{
		val exception = new IOException();
		val channel = new MultipartChannel("boundary",List.<Part>of(new AttachmentPart("contentId","test.txt",() -> new ReadableByteChannel()
		{
			@Override
			public void close()
			{
			}

			@Override
			public boolean isOpen()
			{
				return true;
			}

			@Override
			public int read(ByteBuffer buf) throws IOException
			{
				throw exception;
			}
		},"text/xml")),UTF_8);
		try (channel)
		{
			while (channel.read(ByteBuffer.allocate(1)) != -1)
			{
				// nop
			}
			assertEquals(false,true);
		}
		catch (IOException e)
		{
			assertEquals(exception,e);
		}
	}

	@Test
	void testMultipartFormData() throws Exception
	{
		val httpd = HttpServer.create(new InetSocketAddress("localhost",0),0);
		new Thread(() -> httpd.start()).start();
		try
		{
			EbMSAttachmentFactory.init(null, 1024, null);
			val publisher = new MultipartBodyPublisher("contentId")
					.addXml("contentId", "<content/>")
					.addAttachment(EbMSAttachmentFactory.createEbMSAttachment("test.txt", "test", "text/plain", "Hello world!".getBytes()));
			val client = HttpClient.newHttpClient();
			val request = HttpRequest.newBuilder(new URI("http",null,"localhost",httpd.getAddress().getPort(),"/",null,null))
					.header("Content-Type",publisher.contentType())
					.POST(publisher)
					.build();
			client.send(request,BodyHandlers.discarding());
		}
		finally
		{
			httpd.stop(0);
		}
	}
}
