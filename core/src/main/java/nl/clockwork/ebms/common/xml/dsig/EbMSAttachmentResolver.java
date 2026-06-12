/*
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
package nl.clockwork.ebms.common.xml.dsig;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.common.model.EbMSAttachment;
import nl.clockwork.ebms.common.protocol.Constants;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverContext;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class EbMSAttachmentResolver extends ResourceResolverSpi
{
	@NonNull
	List<EbMSAttachment> attachments;
	@NonNull
	Map<String, EbMSAttachment> attachmentsByContentId;

	public EbMSAttachmentResolver(@NonNull List<EbMSAttachment> attachments)
	{
		this.attachments = attachments;
		val index = new LinkedHashMap<String, EbMSAttachment>(attachments.size() * 2);
		for (val attachment : attachments)
			index.putIfAbsent(attachment.getContentId(), attachment);
		this.attachmentsByContentId = Map.copyOf(index);
	}

	@Override
	public boolean engineCanResolveURI(ResourceResolverContext context)
	{
		if (context.uriToResolve == null || !context.uriToResolve.startsWith(Constants.CID))
			return false;
		return attachmentsByContentId.containsKey(context.uriToResolve.substring(Constants.CID.length()));
	}

	@Override
	public XMLSignatureInput engineResolveURI(ResourceResolverContext context) throws ResourceResolverException
	{
		if (context.uriToResolve == null || !context.uriToResolve.startsWith(Constants.CID))
			throw new ResourceResolverException(
					context.uriToResolve,
					new Object[]{"Reference URI does not start with '" + Constants.CID + "'"},
					context.uriToResolve,
					context.baseUri);
		val attachment = attachmentsByContentId.get(context.uriToResolve.substring(Constants.CID.length()));
		if (attachment == null)
			throw new ResourceResolverException(
					context.uriToResolve,
					new Object[]{"Reference URI = " + context.uriToResolve + " does not exist!"},
					context.uriToResolve,
					context.baseUri);
		try
		{
			val input = new XMLSignatureInput(attachment.getInputStream());
			input.setSourceURI(context.uriToResolve);
			input.setMIMEType(attachment.getContentType());
			return input;
		}
		catch (IOException e)
		{
			throw new ResourceResolverException(e, context.uriToResolve, context.baseUri, context.uriToResolve);
		}
	}
}
