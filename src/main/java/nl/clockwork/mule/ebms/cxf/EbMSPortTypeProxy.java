package nl.clockwork.mule.ebms.cxf;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;

import nl.clockwork.common.cxf.AttachmentOutInterceptor;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.service.EbMS;
import nl.clockwork.ebms.service.EbMSPortType;

public class EbMSPortTypeProxy implements EbMSPortType
{
	private String url;

	public EbMSPortTypeProxy()
	{
	}

	@Override
	public void message(MessageHeader messageHeader, MessageOrder messageOrder, AckRequested ackRequested, ErrorList errorList, Acknowledgment acknowledgment, Manifest manifest, StatusRequest statusRequest, StatusResponse statusResponse)
	{
//		EbMS service = new EbMS();
//		EbMSPortType ebMSPortType = service.getEbMSPort();
//		((BindingProvider)ebMSPortType).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,url);
	
		Service service = Service.create(EbMS.SERVICE);
		service.addPort(EbMS.EbMSPort,SOAPBinding.SOAP11HTTP_BINDING,url);
		EbMSPortType ebMSPortType = service.getPort(EbMSPortType.class);
		
		Client cxfClient = ClientProxy.getClient(ebMSPortType);
		cxfClient.getOutInterceptors().add(new AttachmentOutInterceptor());
	
		ebMSPortType.message(messageHeader,messageOrder,ackRequested,errorList,acknowledgment,manifest,statusRequest,statusResponse);
	}

	@Override
	public void syncMessage(MessageHeader requestMessageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Manifest manifest, StatusRequest statusRequest, Holder<MessageHeader> responseMessageHeader, Holder<ErrorList> errorList, Holder<Acknowledgment> acknowledgment, Holder<StatusResponse> statusResponse)
	{
//		EbMS service = new EbMS();
//		EbMSPortType ebMSPortType = service.getEbMSPort();
//		((BindingProvider)ebMSPortType).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,url);

		Service service = Service.create(EbMS.SERVICE);
		service.addPort(EbMS.EbMSPort,SOAPBinding.SOAP11HTTP_BINDING,url);
		EbMSPortType ebMSPortType = service.getPort(EbMSPortType.class);
		
		Client cxfClient = ClientProxy.getClient(ebMSPortType);
		cxfClient.getOutInterceptors().add(new AttachmentOutInterceptor());
	
		ebMSPortType.syncMessage(requestMessageHeader,syncReply,messageOrder,ackRequested,manifest,statusRequest,responseMessageHeader,errorList,acknowledgment,statusResponse);
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

}
