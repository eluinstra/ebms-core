package nl.clockwork.ebms.validation;

import nl.clockwork.ebms.model.SSLSession;


public class SSLSessionManager 
{
	private static final ThreadLocal<SSLSession> sessionHolder = new ThreadLocal<SSLSession>();

	public static SSLSession getSSLSession()
	{
		return sessionHolder.get();
	}

	public static void setSSLSession(SSLSession sslSession)
	{
		sessionHolder.set(sslSession);
	}

}
