package nl.clockwork.ebms.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.cxf.io.CachedOutputStream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IOUtils
{
	public static CachedOutputStream createCachedOutputStream(InputStream is) throws IOException
	{
		val result = new CachedOutputStream();
		CachedOutputStream.copyStream(is,result,org.apache.commons.io.IOUtils.DEFAULT_BUFFER_SIZE);
		result.lockOutputStream();
		return result;
	}

	public static String toString(org.apache.cxf.io.CachedOutputStream message) throws IOException
	{
		return org.apache.commons.io.IOUtils.toString(new BoundedInputStream(message.getInputStream(),8192),Charset.defaultCharset());
	}
}
