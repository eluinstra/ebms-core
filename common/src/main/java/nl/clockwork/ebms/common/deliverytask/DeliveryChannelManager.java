package nl.clockwork.ebms.common.deliverytask;

import java.util.Optional;

public interface DeliveryChannelManager
{
	public Optional<DeliveryChannel> getDeliveryChannel(String cpaId, String deliveryChannelId);
}
