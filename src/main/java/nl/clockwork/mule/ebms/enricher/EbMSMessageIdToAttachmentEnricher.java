/*******************************************************************************
 * Copyright 2011 Clockwork
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package nl.clockwork.mule.ebms.enricher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import nl.clockwork.common.cxf.AttachmentManager;
import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.message.Attachment;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageIdToAttachmentEnricher extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;

	public EbMSMessageIdToAttachmentEnricher()
	{
		//registerSourceType(Object.class);
	}
	
	@Override
	public Object transform(final MuleMessage message, String outputEncoding) throws TransformerException
	{

		try
		{
			List<DataSource> dataSources = ebMSDAO.getAttachments(message.getLongProperty(Constants.EBMS_MESSAGE_ID,0));
			final Map<String,String> headers = new HashMap<String,String>();
			Collection<Attachment> attachments = new ArrayList<Attachment>();
			for (final DataSource dataSource : dataSources)
			{
				Attachment attachment = new Attachment()
				{
					@Override
					public boolean isXOP()
					{
						return false;
					}
					
					@Override
					public String getId()
					{
						return "1";
					}
					
					@Override
					public Iterator<String> getHeaderNames()
					{
						return headers.keySet().iterator();
					}
					
					@Override
					public String getHeader(String key)
					{
						return headers.get(key);
					}
					
					@Override
					public DataHandler getDataHandler()
					{
						return new DataHandler(dataSource);
					}
				};
				attachments.add(attachment);
			}
			AttachmentManager.set(attachments);

			return message;
		}
		catch (DAOException e)
		{
			throw new TransformerException(this,e);
		}
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
