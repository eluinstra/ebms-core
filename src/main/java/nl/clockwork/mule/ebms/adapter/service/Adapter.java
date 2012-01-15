package nl.clockwork.mule.ebms.adapter.service;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import nl.clockwork.mule.ebms.model.EbMSMessageContent;

@WebService(targetNamespace="http://www.clockwork.nl/ebms/adapter/1.0")
public interface Adapter
{
	@WebResult(name="MessageId")
	String sendMessage(EbMSMessageContent messageContent);

	@WebResult(name="MessageIds")
	List<String> getMessageIds(@WebParam(name="MaxNr") int maxNr);

	@WebResult(name="MessageIds")
	List<String> getMessageIds(@WebParam(name="MaxNr") int maxNr, boolean autoCommit);

	@WebResult(name="Result")
	boolean commitId(@WebParam(name="Id") String id);

	@WebResult(name="Result")
	boolean commitIds(@WebParam(name="Ids") List<String> ids);

	@WebResult(name="Message")
	EbMSMessageContent getMessage(String messageId);
}
