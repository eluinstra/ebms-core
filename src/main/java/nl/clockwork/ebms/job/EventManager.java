package nl.clockwork.ebms.job;

import java.util.Date;

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;

public class EventManager
{
	private EbMSDAO ebMSDAO;
	private CPAManager cpaManager;

	public void createEvent(String cpaId, String deliveryChannelId, String messageId, Date timeToLive, Date timestamp)
	{
		ebMSDAO.insertEvent(new EbMSEvent(cpaId,deliveryChannelId,messageId,timeToLive,timestamp,0));
	}

	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status, final String errorMessage)
	{
		switch (status)
		{
			case FAILED:
				final DeliveryChannel deliveryChannel = cpaManager.getDeliveryChannel(event.getCpaId(),event.getDeliveryChannelId());
				ebMSDAO.executeTransaction(
						new DAOTransactionCallback()
						{
							@Override
							public void doInTransaction()
							{
								ebMSDAO.insertEventLog(event.getMessageId(),event.getTimestamp(),url,status,errorMessage);
								if (CPAUtils.isReliableMessaging(deliveryChannel))
									ebMSDAO.updateEvent(createNewEvent(event,deliveryChannel));
								else
									ebMSDAO.deleteEvent(event.getMessageId());
							}
						}
					);
				break;

			default:
				ebMSDAO.executeTransaction(
						new DAOTransactionCallback()
						{
							@Override
							public void doInTransaction()
							{
								ebMSDAO.insertEventLog(event.getMessageId(),event.getTimestamp(),url,status,errorMessage);
								ebMSDAO.deleteEvent(event.getMessageId());
							}
						}
					);
				break;
		}
	}

	public void deleteEvent(String messageId)
	{
		ebMSDAO.deleteEvent(messageId);
	}

	private EbMSEvent createNewEvent(EbMSEvent event, DeliveryChannel deliveryChannel)
	{
		ReliableMessaging rm = CPAUtils.getReliableMessaging(deliveryChannel);
		Date time = new Date();
		if (event.getRetries() < rm.getRetries().intValue() - 1)
			rm.getRetryInterval().addTo(time);
		else //if (event.getRetries() < rm.getRetries().intValue())
			time = event.getTimeToLive();
		return new EbMSEvent(event.getCpaId(),event.getDeliveryChannelId(),event.getMessageId(),event.getTimeToLive(),time,event.getRetries() + 1);
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

}
