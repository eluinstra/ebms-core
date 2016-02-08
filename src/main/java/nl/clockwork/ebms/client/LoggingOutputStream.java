package nl.clockwork.ebms.client;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class LoggingOutputStream extends FilterOutputStream
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private String charset;
	private StringBuffer sb = new StringBuffer();

	public LoggingOutputStream(OutputStream out)
	{
		this(out,"UTF-8");
	}

	public LoggingOutputStream(OutputStream out, String charset)
	{
		super(out);
		this.charset = charset;
	}

	@Override
	public void write(int b) throws IOException
	{
		if (logger.isDebugEnabled())
			sb.append(b);
		out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		if (logger.isDebugEnabled())
			sb.append(b);
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (logger.isDebugEnabled())
			sb.append(new String(b,off,len,charset));
		out.write(b,off,len);
	}

	@Override
	public void close() throws IOException
	{
		logger.debug(">>>>\n" + sb.toString());
		super.close();
	}
}