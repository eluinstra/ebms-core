package nl.clockwork.ebms.common;

import java.util.Optional;

public interface EbMSAPIDAO
{  
	Optional<EbMSMessageProperties> getEbMSMessageProperties(String messageId);

	Optional<EbMSMessageProperties> getEbMSMessagePropertiesByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);

	Optional<EbMSAction> getMessageAction(String messageId);
}
