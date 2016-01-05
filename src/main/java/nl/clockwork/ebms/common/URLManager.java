package nl.clockwork.ebms.common;

import nl.clockwork.ebms.dao.EbMSDAO;

public class URLManager
{
	private EbMSDAO ebMSDAO;

	public String getUrl(String uri)
	{
		return ebMSDAO.getUrl(uri);
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
