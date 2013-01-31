package nl.clockwork.ebms.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import nl.clockwork.common.util.DOMUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.cpp.cpa.Transport;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

public class EbMSHttpClient implements EbMSClient
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private EbMSSignatureGenerator signatureGenerator;
	private SSLFactoryManager sslFactoryManager;

	public void sendMessage(EbMSMessage ebMSMessage) throws Exception
	{
		Transport transport = getTransport(ebMSMessage);
		URLConnection connection = openConnection(transport);
		if (ebMSMessage.getAttachments().size() > 0)
			createMimeRequest(ebMSMessage,(HttpURLConnection)connection);
		else
			createRequest(ebMSMessage,(HttpURLConnection)connection);
		connection.getOutputStream().flush();
		handleResponse(connection);
	}
	
	private URLConnection openConnection(Transport transport) throws IOException
	{
		URL url = new URL(transport.getTransportReceiver().getEndpoint().get(0).getUri());
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		//connection.setMethod("POST");
		if (connection instanceof HttpsURLConnection)
			((HttpsURLConnection)connection).setSSLSocketFactory(sslFactoryManager.getSslFactory());
		return connection;
	}

	private Transport getTransport(EbMSMessage ebMSMessage)
	{
		CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(ebMSMessage.getMessageHeader().getCPAId());
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,ebMSMessage.getMessageHeader().getTo().getPartyId());
		DeliveryChannel deliveryChannel = CPAUtils.getReceivingDeliveryChannels(partyInfo,ebMSMessage.getMessageHeader().getTo().getRole(),ebMSMessage.getMessageHeader().getService(),ebMSMessage.getMessageHeader().getAction()).get(0);
		return (Transport)deliveryChannel.getTransportId();
	}

	private void handleResponse(URLConnection connection) throws IOException
	{
		//TODO: handle response
		if (connection instanceof HttpURLConnection)
			logger.info("StatusCode: " + ((HttpURLConnection)connection).getResponseCode());
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String s;
		while ((s = in.readLine()) != null)
			logger.info(s);
		in.close();
	}

	private void createRequest(EbMSMessage ebMSMessage, HttpURLConnection connection) throws Exception
	{
		connection.setRequestProperty("Content-Type","text/xml");
		connection.setRequestProperty("SOAPAction","\"ebXML\"");
		Document message = EbMSMessageUtils.createSOAPMessage(ebMSMessage);
		//signatureGenerator.generateSignature(message,ebMSMessage.getAttachments());
		DOMUtils.write(message,connection.getOutputStream());
	}
	
	private void createMimeRequest(EbMSMessage ebMSMessage, HttpURLConnection connection) throws Exception
	{
		String boundary = createBoundary();
		String contentType = createContentType(boundary);

		Document message = EbMSMessageUtils.createSOAPMessage(ebMSMessage);
		signatureGenerator.generateSignature(message,ebMSMessage.getAttachments());

		connection.setRequestProperty("MIME-Version","1.0");
		connection.setRequestProperty("Content-Type",contentType);
		connection.setRequestProperty("SOAPAction","\"ebXML\"");
	
		OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		writer.write("--");
		writer.write(boundary);
		writer.write("\r\n");

		writer.write("Content-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\";");
		writer.write("\r\n");
		writer.write("Content-Transfer-Encoding: binary");
		writer.write("\r\n");
		writer.write("Content-ID: <0>");
		writer.write("\r\n");
		writer.write("\r\n");
		DOMUtils.write(message,writer);
		writer.write("\r\n");
		writer.write("--");
		writer.write(boundary);

		for (EbMSAttachment attachment : ebMSMessage.getAttachments())
		{
			writer.write("\r\n");
			writer.write("Content-Type: " + attachment.getContentType());
			writer.write("\r\n");
			writer.write("Content-Transfer-Encoding: binary");
			writer.write("\r\n");
			writer.write("Content-ID: <" + attachment.getContentId() + ">");
			writer.write("\r\n");
			writer.write("\r\n");
			IOUtils.copy(attachment.getDataSource().getInputStream(),writer);
			writer.write("\r\n");
			writer.write("--");
			writer.write(boundary);
		}
	
		writer.write("--");
		writer.close();
	}

	private String createBoundary()
	{
		return "-=Part.0." + UUID.randomUUID() + "=-";
	}

	private String createContentType(String boundary)
	{
		return "multipart/related; boundary=\"" + boundary + "\"; type=\"text/xml\"; start=\"<0>\"; start-info=\"text/xml\"";
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setSignatureGenerator(EbMSSignatureGenerator signatureGenerator)
	{
		this.signatureGenerator = signatureGenerator;
	}

	public void setSslFactoryManager(SSLFactoryManager sslFactoryManager)
	{
		this.sslFactoryManager = sslFactoryManager;
	}
}
