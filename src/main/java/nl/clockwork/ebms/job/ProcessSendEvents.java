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
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
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
  		EbMSBaseMessage message = ebMSDAO.getMessage(sendEvent.getEbMSMessageId());
  		if (message instanceof EbMSMessage)
  		{
  			AttachmentManager.set(((EbMSMessage)message).getAttachments());
  			//URLManager.set(url);
  			if (message.getSyncReply() == null)
  				ebMSPortType.message(message.getMessageHeader(),((EbMSMessage)message).getMessageOrder(),((EbMSMessage)message).getAckRequested(),null,null,((EbMSMessage)message).getManifest(),null,null);
  			else
  				ebMSPortType.syncMessage(message.getMessageHeader(),message.getSyncReply(),((EbMSMessage)message).getMessageOrder(),((EbMSMessage)message).getAckRequested(),((EbMSMessage)message).getManifest(),null,new Holder<MessageHeader>(),new Holder<ErrorList>(),new Holder<Acknowledgment>(),new Holder<StatusResponse>());
  		}
  		else if (message instanceof EbMSMessageError)
  			ebMSPortType.message(message.getMessageHeader(),null,null,((EbMSMessageError)message).getErrorList(),null,null,null,null);
  		else if (message instanceof EbMSAcknowledgment)
  			ebMSPortType.message(message.getMessageHeader(),null,null,null,((EbMSAcknowledgment)message).getAcknowledgment(),null,null,null);
  		else if (message instanceof EbMSStatusRequest)
  			if (message.getSyncReply() == null)
  				ebMSPortType.message(message.getMessageHeader(),null,null,null,null,null,((EbMSStatusRequest)message).getStatusRequest(),null);
  			else
  				ebMSPortType.syncMessage(message.getMessageHeader(),message.getSyncReply(),null,null,null,((EbMSStatusRequest)message).getStatusRequest(),new Holder<MessageHeader>(),new Holder<ErrorList>(),new Holder<Acknowledgment>(),new Holder<StatusResponse>());
  		else if (message instanceof EbMSStatusResponse)
  			ebMSPortType.message(message.getMessageHeader(),null,null,null,null,null,null,((EbMSStatusResponse)message).getStatusResponse());
  		else if (message instanceof EbMSPing)
  			if (message.getSyncReply() == null)
  				ebMSPortType.message(message.getMessageHeader(),null,null,null,null,null,null,null);
  			else
  				ebMSPortType.syncMessage(message.getMessageHeader(),message.getSyncReply(),null,null,null,null,new Holder<MessageHeader>(),new Holder<ErrorList>(),new Holder<Acknowledgment>(),new Holder<StatusResponse>());
  		else if (message instanceof EbMSPong)
				ebMSPortType.message(message.getMessageHeader(),null,null,null,null,null,null,null);
  		ebMSDAO.deleteEventsForSending(timestamp,sendEvent.getEbMSMessageId());
  	}
  }
  
  public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
  
  public void setEbMSPortType(EbMSPortType ebMSPortType)
	{
		this.ebMSPortType = ebMSPortType;
	}
}
