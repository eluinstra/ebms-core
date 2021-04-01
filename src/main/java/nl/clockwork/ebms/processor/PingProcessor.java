package nl.clockwork.ebms.processor;

import java.io.IOException;
import java.time.Instant;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.validation.EbMSMessageValidator;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class PingProcessor extends StatelessMessageProcessor
{
	@NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageValidator messageValidator;
  @NonNull
	PongProcessor pongProcessor;

  public EbMSDocument processPing(Instant timestamp, EbMSPing ping) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		messageValidator.validate(ping,timestamp);
		val pong = ping.createPong(cpaManager,timestamp);
		if (ping.isSyncReply(cpaManager))
			return EbMSMessageUtils.getEbMSDocument(pong);
		else
		{
			pongProcessor.sendPong(pong);
			return null;
		}
	}
}
