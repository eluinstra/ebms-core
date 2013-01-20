package nl.clockwork.ebms.job;

import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.ws.Holder;

import nl.clockwork.ebms.AttachmentManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.service.EbMSPortType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessSendEvents
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;
  private EbMSPortType ebMSPortType;

  public void process()
  {
  	GregorianCalendar timestamp = new GregorianCalendar();
  	List<EbMSSendEvent> sendEvents = ebMSDAO.selectEventsForSending(timestamp);
  	for (EbMSSendEvent sendEvent : sendEvents)
  	{
  		EbMSBaseMessage message = ebMSDAO.getMessage(sendEvent.getMessageId());
  		if (message instanceof EbMSMessage)
  		{
  			AttachmentManager.set(((EbMSMessage)message).getAttachments());
  			ebMSPortType.message(message.getMessageHeader(),((EbMSMessage)message).getSyncReply(),((EbMSMessage)message).getMessageOrder(),((EbMSMessage)message).getAckRequested(),((EbMSMessage)message).getManifest());
  		}
  		else if (message instanceof EbMSMessageError)
  			ebMSPortType.messageError(message.getMessageHeader(),((EbMSMessageError)message).getErrorList());
  		else if (message instanceof EbMSAcknowledgment)
  			ebMSPortType.acknowledgment(message.getMessageHeader(),((EbMSAcknowledgment)message).getAcknowledgment());
  		else if (message instanceof EbMSStatusRequest)
  			ebMSPortType.messageStatus(message.getMessageHeader(),((EbMSStatusRequest)message).getSyncReply(),((EbMSStatusRequest)message).getStatusRequest(),new Holder<MessageHeader>(), new Holder<StatusResponse>());
  		//else if (message instanceof EbMSStatusResponse)
  			//ebMSPortType.messageStatusResponse(message.getMessageHeader(),((EbMSStatusResponse)message).getStatusResponse());
  		else if (message instanceof EbMSPing)
  			ebMSPortType.ping(message.getMessageHeader(),((EbMSPing)message).getSyncReply());
  		//else if (message instanceof EbMSPong)
  			//ebMSPortType.pong(message.getMessageHeader());
  	}
  }
}
