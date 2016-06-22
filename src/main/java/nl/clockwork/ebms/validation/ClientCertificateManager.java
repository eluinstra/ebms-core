package nl.clockwork.ebms.validation;

import java.security.cert.X509Certificate;

public class ClientCertificateManager
{
	private static final ThreadLocal<X509Certificate[]> sessionHolder = new ThreadLocal<X509Certificate[]>();
	
	public static X509Certificate[] getCertificates()
	{
		return sessionHolder.get();
	}

	public static void setCertificates(X509Certificate[] certificates)
	{
		sessionHolder.set(certificates);
	}
}
