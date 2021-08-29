package nl.clockwork.ebms.cpa.certificate;

public class CertificateNotFoundException extends CertificateMappingServiceException
{
	private static final long serialVersionUID = 1L;

	public CertificateNotFoundException()
	{
		super("Certificate not found");
	}
}
