package nl.clockwork.ebms.validation;

import nl.clockwork.ebms.model.SSLSession;


public class SSLSessionManager 
{
	private static final ThreadLocal<SSLSession> certificateHolder = new ThreadLocal<SSLSession>();

	public static SSLSession getSSLSession()
	{
		return certificateHolder.get();
	}

	public static void setSSLSession(SSLSession sslSession)
	{
		certificateHolder.set(sslSession);
	}

}
