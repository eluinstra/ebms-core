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
package nl.clockwork.ebms.validation;

import java.util.List;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Manifest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Reference;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SeverityType;

public class ManifestValidator
{
  protected transient Log logger = LogFactory.getLog(getClass());

	public boolean isValid(ErrorList errorList, Manifest manifest, List<EbMSAttachment> attachments)
	{
		if (!Constants.EBMS_VERSION.equals(manifest.getVersion()))
		{
			errorList.getError().add(EbMSMessageUtils.createError("//Body/Manifest[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Invalid value."));
			errorList.setHighestSeverity(SeverityType.ERROR);
			return false;
		}
		for (Reference reference : manifest.getReference())
		{
			if (reference.getHref().startsWith(Constants.CID))
			{
				boolean found = false;
				for (EbMSAttachment attachment : attachments)
					if (reference.getHref().substring(Constants.CID.length()).equals(attachment.getContentId()))
						found = true;
				if (!found)
				{
					errorList.getError().add(EbMSMessageUtils.createError(reference.getHref(),Constants.EbMSErrorCode.MIME_PROBLEM.errorCode(),"MIME part not found."));
					errorList.setHighestSeverity(SeverityType.ERROR);
					return false;
				}
			}
			else
			{
				errorList.getError().add(EbMSMessageUtils.createError("//Body/Manifest/Reference[@href='" + reference.getHref() + "']",Constants.EbMSErrorCode.MIME_PROBLEM.errorCode(),"URI cannot be resolved."));
				errorList.setHighestSeverity(SeverityType.ERROR);
				return false;
			}
		}
		return true;
	}
}
