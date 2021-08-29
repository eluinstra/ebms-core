package nl.clockwork.ebms.cpa;

public class CPANotFoundException extends CPAServiceException
{
	private static final long serialVersionUID = 1L;

	public CPANotFoundException()
	{
		super("CPA not found");
	}
}
