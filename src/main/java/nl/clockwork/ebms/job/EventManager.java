package nl.clockwork.ebms.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.clockwork.ebms.Constants.EbMSEventType;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;

public class EventManager
{
	private CPAManager cpaManager;

	public EbMSEvent createEbMSSendEvent(EbMSMessage message, String uri)
	{
		return new EbMSEvent(message.getMessageHeader().getMessageData().getMessageId(),message.getMessageHeader().getMessageData().getTimestamp(),EbMSEventType.SEND,uri);
	}

	public List<EbMSEvent> createEbMSSendEvents(String cpaId, EbMSMessage message, String uri)
	{
		List<EbMSEvent> result = new ArrayList<EbMSEvent>();
		Date sendTime = message.getMessageHeader().getMessageData().getTimestamp();
		DeliveryChannel deliveryChannel = cpaManager.getFromDeliveryChannel(cpaId,new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction());
		if (CPAUtils.isReliableMessaging(deliveryChannel))
		{
			ReliableMessaging rm = CPAUtils.getReliableMessaging(deliveryChannel);
			for (int i = 0; i < rm.getRetries().intValue() + 1; i++)
			{
				result.add(new EbMSEvent(message.getMessageHeader().getMessageData().getMessageId(),(Date)sendTime.clone(),EbMSEventType.SEND,uri));
				rm.getRetryInterval().addTo(sendTime);
			}
			if (message.getMessageHeader().getMessageData().getTimeToLive() == null)
				result.add(new EbMSEvent(message.getMessageHeader().getMessageData().getMessageId(),(Date)sendTime.clone(),EbMSEventType.EXPIRE));
			else
				result.add(new EbMSEvent(message.getMessageHeader().getMessageData().getMessageId(),message.getMessageHeader().getMessageData().getTimeToLive(),EbMSEventType.EXPIRE));
		}
		else
			result.add(new EbMSEvent(message.getMessageHeader().getMessageData().getMessageId(),(Date)sendTime.clone(),EbMSEventType.SEND,uri));
		return result;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}
}