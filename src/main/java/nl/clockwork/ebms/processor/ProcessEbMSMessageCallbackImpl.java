package nl.clockwork.ebms.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessageContent;

public class ProcessEbMSMessageCallbackImpl implements ProcessEbMSMessageCallback
{
  protected transient Log logger = LogFactory.getLog(getClass());

  public void process(EbMSMessageContent messageContent)
	{
		logger.info("Received message: " + messageContent.getContext().getMessageId());
		for (EbMSDataSource dataSource : messageContent.getDataSources())
			logger.info(new String(dataSource.getContent()));
	}
}
