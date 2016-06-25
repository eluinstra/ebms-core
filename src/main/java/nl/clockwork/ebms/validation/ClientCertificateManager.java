package nl.clockwork.ebms.validation;

import java.security.cert.X509Certificate;

public class ClientCertificateManager
{
	private static final ThreadLocal<X509Certificate[]> certificatesHolder = new ThreadLocal<X509Certificate[]>();
	
	public static X509Certificate[] getCertificates()
	{
		return certificatesHolder.get();
	}

	public static void setCertificates(X509Certificate[] certificates)
	{
		certificatesHolder.set(certificates);
	}
}
