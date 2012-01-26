package nl.clockwork.mule.ebms.model;

public class EbMSAction
{
	private String service;
	private String action;
	
	public EbMSAction(String service, String action)
	{
		this.service = service;
		this.action = action;
	}
	
	public String getService()
	{
		return service;
	}
	
	public String getAction()
	{
		return action;
	}
}
