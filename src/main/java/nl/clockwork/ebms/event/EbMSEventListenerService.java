package nl.clockwork.ebms.event;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;


@WebService(targetNamespace="http://www.clockwork.nl/ebms/event/2.0")
public interface EbMSEventListenerService extends EventListener
{

	@Override
	@WebMethod(operationName="MessageReceived")
	public void onMessageReceived(@WebParam(name="MessageId") String messageId) throws EbMSEventListenerServiceException;

	@Override
	@WebMethod(operationName="MessageAcknowledged")
	public void onMessageAcknowledged(@WebParam(name="MessageId") String messageId) throws EbMSEventListenerServiceException;

	@Override
	@WebMethod(operationName="MessageDeliveryFailed")
	public void onMessageDeliveryFailed(@WebParam(name="MessageId") String messageId) throws EbMSEventListenerServiceException;

	@Override
	@WebMethod(operationName="MessageNotAcknowledged")
	public void onMessageNotAcknowledged(@WebParam(name="MessageId") String messageId) throws EbMSEventListenerServiceException;

}
