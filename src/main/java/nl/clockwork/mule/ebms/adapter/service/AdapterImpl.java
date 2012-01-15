package nl.clockwork.mule.ebms.adapter.service;

import java.util.List;

import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

public class AdapterImpl implements Adapter
{
	private EbMSDAO ebMSDAO;
	private String hostname;

	@Override
	public String sendMessage(EbMSMessageContent messageContent)
	{
		try
		{
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageContent.getContext().getCpaId());
			EbMSMessage message = EbMSMessageUtils.ebMSMessageContentToEbMSMessage(cpa,messageContent,hostname);
			ebMSDAO.insertMessage(message);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public List<String> getMessageIds(int maxNr)
	{
		return null;
	}

	@Override
	public EbMSMessageContent getMessage(String messageId, boolean autoCommit)
	{
		return null;
	}

	@Override
	public boolean commitId(String id)
	{
		return false;
	}

	@Override
	public boolean commitIds(List<String> ids)
	{
		return false;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}
}
