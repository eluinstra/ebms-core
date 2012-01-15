package nl.clockwork.mule.ebms.model;

public class EbMSAttachment
{
	private String name;
	private String contentType;
	private byte[] content;

	public EbMSAttachment(String name, String contentType, byte[] content)
	{
		this.name = name;
		this.contentType = contentType;
		this.content = content;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getContentType()
	{
		return contentType;
	}
	
	public byte[] getContent()
	{
		return content;
	}
}
