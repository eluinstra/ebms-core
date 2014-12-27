package nl.clockwork.ebms.service;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class GetCPAIdsResponse
{
	private List<String> cpaIds;

	public GetCPAIdsResponse()
	{
	}

	@XmlElementWrapper(nillable=true,name="CPAIds")
	@XmlElement(name="CPAId")
	public List<String> getCpaIds()
	{
		return cpaIds;
	}
	
	public void setCpaIds(List<String> cpaIds)
	{
		this.cpaIds = cpaIds;
	}
}
