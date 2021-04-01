package nl.clockwork.ebms.processor;

import java.io.IOException;
import java.time.Instant;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
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
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.validation.EbMSMessageValidator;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class StatusRequestProcessor
{
	@NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageValidator messageValidator;
  @NonNull
	StatusResponseProcessor statusResponseProcessor;

  public EbMSDocument processStatusRequest(Instant timestamp, EbMSStatusRequest statusRequest) throws DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		messageValidator.validate(statusRequest,timestamp);
		val statusResponse = statusResponseProcessor.createStatusResponse(statusRequest,timestamp);
		if (statusRequest.isSyncReply(cpaManager))
			return EbMSMessageUtils.getEbMSDocument(statusResponse);
		else
		{
			statusResponseProcessor.sendStatusResponse(statusResponse);
			return null;
		}
	}

}
