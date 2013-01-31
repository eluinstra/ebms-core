package nl.clockwork.ebms.job;

import java.util.GregorianCalendar;
import java.util.List;

import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.EbMSStatusRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessSendEvents implements Job
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;
  private EbMSClient ebMSClient;

  @Override
  public void execute()
  {
  	try
  	{
	  	GregorianCalendar timestamp = new GregorianCalendar();
	  	List<EbMSSendEvent> sendEvents = ebMSDAO.selectEventsForSending(timestamp);
	  	for (EbMSSendEvent sendEvent : sendEvents)
	  	{
	  		EbMSBaseMessage message = ebMSDAO.getMessage(sendEvent.getEbMSMessageId());

	  		if (message instanceof EbMSMessageError)
	  			message = new EbMSMessage(message.getMessageHeader(),((EbMSMessageError)message).getErrorList());
	  		else if (message instanceof EbMSAcknowledgment)
	  			message = new EbMSMessage(message.getMessageHeader(),((EbMSAcknowledgment)message).getAcknowledgment());
	  		else if (message instanceof EbMSStatusRequest)
	  			message = new EbMSMessage(message.getMessageHeader(),null,((EbMSStatusRequest)message).getStatusRequest());
	  		else if (message instanceof EbMSMessageError)
	  			message = new EbMSMessage(message.getMessageHeader(),((EbMSMessageError)message).getErrorList());

	  		ebMSClient.sendMessage((EbMSMessage)message);
	  		ebMSDAO.deleteEventsForSending(timestamp,sendEvent.getEbMSMessageId());
	  	}
  	}
  	catch (Exception e)
  	{
  		logger.error("",e);
  	}
  }
  
  public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

  public void setEbMSClient(EbMSClient ebMSClient)
	{
		this.ebMSClient = ebMSClient;
	}
}
