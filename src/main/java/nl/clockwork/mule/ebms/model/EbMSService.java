package nl.clockwork.mule.ebms.model;

import nl.clockwork.mule.ebms.model.ebxml.Service;

public class EbMSService extends Service
{
	public EbMSService(String type, String value)
	{
		this.type = type;
		this.value = value;
	}
	
	public boolean compare(Service service)
	{
		if (service == null)
			return false;
		return (this.type == service.getType() || (this.type != null && this.type.equals(service.getType())))
				&& (this.value == null || service.getValue() == null || this.value.equals(service.getValue()));
	}
}
