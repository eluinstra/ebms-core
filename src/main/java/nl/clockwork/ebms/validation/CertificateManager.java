package nl.clockwork.ebms.validation;

import javax.security.cert.X509Certificate;

public class CertificateManager 
{
	private static final ThreadLocal<X509Certificate> certificateHolder = new ThreadLocal<X509Certificate>();

	public static X509Certificate getCertificate()
	{
		return certificateHolder.get();
	}

	public static void setCertificate(X509Certificate certificate)
	{
		certificateHolder.set(certificate);
	}

}
