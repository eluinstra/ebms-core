package nl.clockwork.ebms.cpa.url;

public class URLNotFoundException extends URLMappingServiceException
{
	private static final long serialVersionUID = 1L;

	public URLNotFoundException()
	{
		super("URL not found");
	}
}
