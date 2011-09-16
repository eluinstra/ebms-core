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
package nl.clockwork.mule.ebms.filter;

import javax.activation.DataSource;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.model.EbMSDataSource;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.Reference;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class EbMSMessageBodyValidationFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public boolean accept(MuleMessage message)
	{
		if (message.getPayload() instanceof EbMSMessage)
		{
			EbMSMessage msg = (EbMSMessage)message.getPayload();
			Manifest manifest = msg.getManifest();
			if (!Constants.EBMS_VERSION.equals(manifest.getVersion()))
			{
				message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Body/Manifest[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
				return false;
			}
			if (manifest.getReference().size() != msg.getAttachments().size())
			{
				message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Body/Manifest",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong number of attachments."));
				return false;
			}
			for (Reference reference : manifest.getReference())
			{
				boolean found = false;
				for (DataSource dataSource : msg.getAttachments())
					if (reference.getHref().startsWith("cid:") && reference.getHref().substring("cid:".length()).equals(((EbMSDataSource)dataSource).getContentId()))
						found = true;
				if (!found)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Body/Manifest/Reference[@href='" + reference.getHref() + "']",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Attachment not found."));
					return false;
				}
			}
		}
		return true;
	}
}
