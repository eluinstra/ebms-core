package nl.clockwork.ebms.client;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

public class DeliveryManager //DeliveryService
{
	protected transient Log logger = LogFactory.getLog(getClass());
  private ExecutorService executorService;
  private int maxThreads = 4;
  private MessageQueue messageQueue;
	private EbMSClient ebMSClient;

	public void init()
	{
		executorService = Executors.newFixedThreadPool(maxThreads);
	}

	public EbMSDocument sendMessage(final CollaborationProtocolAgreement cpa, final EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			final String uri = CPAUtils.getUri(cpa,message);
			if (message.getSyncReply() == null)
			{
				Runnable command = new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							ebMSClient.sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
						}
						catch (Exception e)
						{
							messageQueue.putEmptyMessage(message);
							logger.error("",e);
						}
					}
				};
				messageQueue.register(message);
				executorService.execute(command);
				EbMSMessage response = messageQueue.getMessage(message);
				if (response != null)
					return EbMSMessageUtils.getEbMSDocument(response);
			}
			else
				return ebMSClient.sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
			return null;
		}
		catch (EbMSProcessorException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	public EbMSDocument handleResponseMessage(final CollaborationProtocolAgreement cpa, final EbMSMessage message, final EbMSMessage response) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		if (response != null)
		{
			if (message.getSyncReply() == null)
			{
				Runnable command = new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							String uri = CPAUtils.getUri(cpa,response);
							ebMSClient.sendMessage(uri,EbMSMessageUtils.getEbMSDocument(response));
						}
						catch (Exception e)
						{
							logger.error("",e);
						}
					}
				};
				executorService.execute(command);
			}
			else
				return EbMSMessageUtils.getEbMSDocument(response);
		}
		return null;
	}

	public void setMaxThreads(int maxThreads)
	{
		this.maxThreads = maxThreads;
	}

	public void setMessageQueue(MessageQueue messageQueue)
	{
		this.messageQueue = messageQueue;
	}

	public void setEbMSClient(EbMSClient ebMSClient)
	{
		this.ebMSClient = ebMSClient;
	}

}
