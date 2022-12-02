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

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.val;
import nl.clockwork.ebms.model.EbMSAttachment;

@Value
public class MultipartBodyPublisher implements BodyPublisher
{
	private static String nextBoundary()
	{
		return String.format("-=%s=-",UUID.randomUUID());
	}

	String contentId;
	String boundary = nextBoundary();
	List<Part> parts = new ArrayList<>();
	Charset charset;
	BodyPublisher delegate;

	public MultipartBodyPublisher(String contentId)
	{
		this(contentId,StandardCharsets.UTF_8);
	}

	public MultipartBodyPublisher(String contentId, Charset charset)
	{
		this.contentId = contentId;
		this.charset = charset;
		delegate = BodyPublishers.ofInputStream(() -> Channels.newInputStream(new MultipartChannel(this.boundary,this.parts,this.charset)));
	}

	private MultipartBodyPublisher add(Part part)
	{
		parts.add(part);
		return this;
	}

	public MultipartBodyPublisher addXml(String contentId, String value)
	{
		return add(new XmlPart(contentId,value,charset));
	}

	public MultipartBodyPublisher addAttachment(EbMSAttachment attachment)
	{
		return add(new AttachmentPart(attachment.getContentId(),attachment.getName(),() -> channel(attachment),attachment.getContentType()));
	}

	private ReadableByteChannel channel(EbMSAttachment attachment)
	{
		try
		{
			return Channels.newChannel(attachment.getInputStream());
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public String contentType()
	{
		return String.format("multipart/related; boundary=\"%s\"; type=\"text/xml\"; start=\"<%s>\"; start-info=\"text/xml\"",boundary,contentId);
	}

	@Override
	public void subscribe(Subscriber<? super ByteBuffer> s)
	{
		delegate.subscribe(s);
	}

	@Override
	public long contentLength()
	{
		return delegate.contentLength();
	}
}

interface Part
{
	String getContentId();

	ReadableByteChannel open() throws IOException;
}

@Value
@AllArgsConstructor
class XmlPart implements Part
{
	String contentId;
	String value;
	Charset charset;

	public XmlPart(String contentId, String value)
	{
		this(contentId,value,StandardCharsets.UTF_8);
	}

	@Override
	public ReadableByteChannel open() throws IOException
	{
		val input = new ByteArrayInputStream(value.getBytes(charset));
		return Channels.newChannel(input);
	}
}

@Value
@AllArgsConstructor
class AttachmentPart implements Part
{
	String contentId;
	String filename;
	Supplier<ReadableByteChannel> supplier;
	String contentType;

	@Override
	public ReadableByteChannel open() throws IOException
	{
		return supplier.get();
	}
}

enum State
{
	BOUNDARY, HEADERS, BODY, DONE,
}

class MultipartChannel implements ReadableByteChannel
{
	private static final Charset LATIN1 = StandardCharsets.ISO_8859_1;
	private boolean closed = false;
	private State state = State.BOUNDARY;
	private final String boundary;
	private final Iterator<Part> parts;
	private ByteBuffer buf = ByteBuffer.allocate(0);
	private Part current = null;
	private ReadableByteChannel channel = null;
	private final Charset charset;

	MultipartChannel(String boundary, Iterable<Part> parts, Charset charset)
	{
		this.boundary = boundary;
		this.parts = parts.iterator();
		this.charset = charset;
	}

	@Override
	public void close() throws IOException
	{
		if (channel != null)
		{
			channel.close();
			channel = null;
		}
		closed = true;
	}

	@Override
	public boolean isOpen()
	{
		return !closed;
	}

	@Override
	public int read(ByteBuffer buf) throws IOException
	{
		while (true)
		{
			if (this.buf.hasRemaining())
			{
				val n = Math.min(this.buf.remaining(),buf.remaining());
				val slice = this.buf.slice();
				slice.limit(n);
				buf.put(slice);
				this.buf.position(this.buf.position() + n);
				return n;
			}

			switch (state)
			{
				case BOUNDARY:
					if (parts.hasNext())
					{
						current = parts.next();
						this.buf = ByteBuffer.wrap(("--" + boundary + "\r\n").getBytes(LATIN1));
						state = State.HEADERS;
					}
					else
					{
						this.buf = ByteBuffer.wrap(("--" + boundary + "--\r\n").getBytes(LATIN1));
						state = State.DONE;
					}
					break;
				case HEADERS:
					this.buf = ByteBuffer.wrap(currentHeaders().getBytes(charset));
					state = State.BODY;
					break;
				case BODY:
					if (channel == null)
						channel = current.open();
					val n = channel.read(buf);
					if (n == -1)
					{
						channel.close();
						channel = null;
						this.buf = ByteBuffer.wrap("\r\n".getBytes(LATIN1));
						state = State.BOUNDARY;
					}
					else
						return n;
					break;
				case DONE:
					return -1;
			}
		}
	}

	String currentHeaders()
	{
		return Match(current).of(
			Case($(instanceOf(XmlPart.class)),this::headers),
			Case($(instanceOf(AttachmentPart.class)),this::headers),
			Case($(),() -> { throw new IllegalStateException(); }));
	}

	private String headers(XmlPart part)
	{
		val format = new StringJoiner("\r\n","","\r\n").add("Content-Type: text/xml; charset=UTF-8").add("Content-ID: <%s>").toString();
		return String.format(format,part.getContentId()) + "\r\n";
	}

	private String headers(AttachmentPart part)
	{
		val joiner = new StringJoiner("\r\n","","\r\n").add("Content-Type: %s").add("Content-ID: <%s>").add("Content-Disposition: attachment; filename=\"%s\"");
		val format = part.getContentType().matches("^(text/.*|.*/xml)$") ? joiner.toString() : joiner.add("Content-Transfer-Encoding: binary").toString();
		return String.format(format,part.getContentType(),part.getContentId(),part.getFilename()) + "\r\n";
	}
}