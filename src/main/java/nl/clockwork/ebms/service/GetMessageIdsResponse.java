package nl.clockwork.ebms.service;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class GetMessageIdsResponse
{
	private List<String> messageIds;

	public GetMessageIdsResponse()
	{
	}

	@XmlElementWrapper(nillable=true,name="MessageIds")
	@XmlElement(name="MessageId")
	public List<String> getMessageIds()
	{
		return messageIds;
	}
	
	public void setMessageIds(List<String> messageIds)
	{
		this.messageIds = messageIds;
	}
}
