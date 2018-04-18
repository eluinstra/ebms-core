/**
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
 */
package nl.clockwork.ebms.validation;

import java.util.ArrayList;
import java.util.List;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Reference;

public class ManifestValidator
{

	public void validate(EbMSMessage message) throws EbMSValidationException
	{
		List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
		if (message.getManifest() != null)
		{
			if (!Constants.EBMS_VERSION.equals(message.getManifest().getVersion()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Body/Manifest/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			for (Reference reference : message.getManifest().getReference())
			{
				if (reference.getHref().startsWith(Constants.CID))
				{
					EbMSAttachment attachment = findAttachment(message.getAttachments(),reference.getHref());
					if (attachment != null)
						attachments.add(attachment);
					else
						throw new EbMSValidationException(EbMSMessageUtils.createError(reference.getHref(),Constants.EbMSErrorCode.MIME_PROBLEM,"MIME part not found."));
				}
				else
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Body/Manifest/Reference[@href='" + reference.getHref() + "']",Constants.EbMSErrorCode.MIME_PROBLEM,"URI cannot be resolved."));
			}
		}
		message.getAttachments().retainAll(attachments);
	}

	private EbMSAttachment findAttachment(List<EbMSAttachment> attachments, String href)
	{
		for (EbMSAttachment attachment : attachments)
			if (href.substring(Constants.CID.length()).equals(attachment.getContentId()))
				return attachment;
		return null;
	}
}
