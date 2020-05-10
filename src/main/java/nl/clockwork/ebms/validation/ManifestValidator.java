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

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Reference;

import lombok.val;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSErrorCode;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;

class ManifestValidator
{
	public void validate(EbMSMessage message) throws EbMSValidationException
	{
		val attachments = new ArrayList<EbMSAttachment>();
		if (message.getManifest() != null)
		{
			if (!Constants.EBMS_VERSION.equals(message.getManifest().getVersion()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Body/Manifest/@version",EbMSErrorCode.INCONSISTENT,"Invalid value."));
			for (Reference reference : message.getManifest().getReference())
				addAttachment(attachments,message.getAttachments(),reference);
		}
		message.getAttachments().retainAll(attachments);
	}

	private void addAttachment(List<EbMSAttachment> destAttachments, List<EbMSAttachment> srcAttachments, Reference reference)
	{
		if (reference.getHref().startsWith(Constants.CID))
		{
			val attachment = findAttachment(srcAttachments,reference.getHref());
			if (attachment != null)
				destAttachments.add(attachment);
			else
				throw new EbMSValidationException(EbMSMessageUtils.createError(reference.getHref(),EbMSErrorCode.MIME_PROBLEM,"MIME part not found."));
		}
		else
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Body/Manifest/Reference[@href='" + reference.getHref() + "']",EbMSErrorCode.MIME_PROBLEM,"URI cannot be resolved."));
	}

	private EbMSAttachment findAttachment(List<EbMSAttachment> attachments, String href)
	{
		return attachments.stream()
				.filter(a -> href.substring(Constants.CID.length()).equals(a.getContentId()))
				.findFirst()
				.orElse(null);
	}
}
