package nl.clockwork.ebms.client;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import nl.clockwork.ebms.common.MessageQueue;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.processor.EbMSProcessingException;
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
  private MessageQueue<EbMSMessage> messageQueue;
	private EbMSClient ebMSClient;

	public void init()
	{
		executorService = Executors.newFixedThreadPool(maxThreads);
	}

	public EbMSMessage sendMessage(final CollaborationProtocolAgreement cpa, final EbMSMessage message) throws EbMSProcessorException
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
							messageQueue.put(message.getMessageHeader().getMessageData().getMessageId(),null);
							logger.error("",e);
						}
					}
				};
				messageQueue.register(message.getMessageHeader().getMessageData().getMessageId());
				executorService.execute(command);
				EbMSMessage response = messageQueue.get(message.getMessageHeader().getMessageData().getMessageId());
				if (response != null)
					return response;
			}
			else
			{
				EbMSDocument response = ebMSClient.sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
				if (response != null)
					return EbMSMessageUtils.getEbMSMessage(response.getMessage(),response.getAttachments());
			}
			return null;
		}
		catch (SOAPException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (JAXBException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (SAXException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (IOException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (XPathExpressionException e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	public void handleResponseMessage(final EbMSMessage message) throws EbMSProcessorException
	{
		messageQueue.put(message.getMessageHeader().getMessageData().getRefToMessageId(),message);
	}
	
	public EbMSMessage handleResponseMessage(final CollaborationProtocolAgreement cpa, final EbMSMessage message, final EbMSMessage response) throws EbMSProcessorException
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
				return response;
		}
		return null;
	}

	public void setMaxThreads(int maxThreads)
	{
		this.maxThreads = maxThreads;
	}

	public void setMessageQueue(MessageQueue<EbMSMessage> messageQueue)
	{
		this.messageQueue = messageQueue;
	}

	public void setEbMSClient(EbMSClient ebMSClient)
	{
		this.ebMSClient = ebMSClient;
	}

}
