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
package nl.clockwork.ebms.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import nl.clockwork.ebms.validation.ValidationException;

/**
 * An {@link InputStream} wrapper that throws a {@link ValidationException} once more than
 * {@code maxBytes} bytes have been read from the underlying stream.
 *
 * <p>This guards the EbMS message endpoint against oversized (denial-of-service) payloads.
 * Unlike a {@code Content-Length} check it is robust to missing or lying length headers
 * and to chunked transfer, because the cap is enforced on the bytes actually read.
 */
public class LimitedInputStream extends FilterInputStream
{
	private static final long serialVersionUID = 1L;
	private final long maxBytes;
	private long bytesRead;

	public LimitedInputStream(InputStream in, long maxBytes)
	{
		super(in);
		if (maxBytes < 0)
			throw new IllegalArgumentException("maxBytes must be >= 0");
		this.maxBytes = maxBytes;
	}

	public long getMaxBytes()
	{
		return maxBytes;
	}

	@Override
	public int read() throws IOException
	{
		int result = super.read();
		if (result != -1)
			count(1);
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		int result = super.read(b, off, len);
		if (result > 0)
			count(result);
		return result;
	}

	private void count(long n)
	{
		bytesRead += n;
		if (bytesRead > maxBytes)
			throw new ValidationException("Message exceeds the maximum allowed size of " + maxBytes + " bytes");
	}
}
