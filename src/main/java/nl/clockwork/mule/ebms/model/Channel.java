package nl.clockwork.mule.ebms.model;

public class Channel
{
	private int id;
	private String channelId;
	private String cpaId;
	private String actionId;
	private String endpoint;

	public Channel(int id, String channelId, String cpaId, String actionId, String endpoint)
	{
		super();
		this.id = id;
		this.channelId = channelId;
		this.cpaId = cpaId;
		this.actionId = actionId;
		this.endpoint = endpoint;
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	public String getChannelId()
	{
		return channelId;
	}
	public void setChannelId(String channelId)
	{
		this.channelId = channelId;
	}
	public String getCpaId()
	{
		return cpaId;
	}
	public void setCpaId(String cpaId)
	{
		this.cpaId = cpaId;
	}
	public String getActionId()
	{
		return actionId;
	}
	public void setActionId(String actionId)
	{
		this.actionId = actionId;
	}
	public String getEndpoint()
	{
		return endpoint;
	}
	public void setEndpoint(String endpoint)
	{
		this.endpoint = endpoint;
	}

}
