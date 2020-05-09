package nl.clockwork.ebms.processor;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.ValidatorException;

@Builder
@CommonsLog
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class PongProcessor
{
  @NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageValidator messageValidator;
  @NonNull
	EbMSMessageFactory ebMSMessageFactory;
  @NonNull
  DeliveryManager deliveryManager;

  public EbMSPong createPong(EbMSPing message, Instant timestamp) throws ValidatorException, EbMSProcessorException
	{
		return ebMSMessageFactory.createEbMSPong(message);
	}

  public void sendPong(final nl.clockwork.ebms.model.EbMSPong pong)
	{
		val responseMessageHeader = pong.getMessageHeader();
		val toPartyId = new CacheablePartyId(responseMessageHeader.getTo().getPartyId());
		val service = CPAUtils.toString(responseMessageHeader.getService());
		val uri = cpaManager.getUri(
				responseMessageHeader.getCPAId(),
				toPartyId,
				responseMessageHeader.getTo().getRole(),
				service,
				responseMessageHeader.getAction());
		deliveryManager.sendResponseMessage(uri,pong);
	}

  public void processPong(Instant timestamp, EbMSPong pong)
	{
		try
		{
			messageValidator.validate(pong,timestamp);
			deliveryManager.handleResponseMessage(pong);
		}
		catch (ValidatorException e)
		{
			log.warn("Unable to process Pong " + pong.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}
}
