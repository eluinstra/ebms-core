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
package nl.clockwork.ebms.xml.dsig;

import java.io.IOException;
import java.util.List;

import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverContext;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSAttachment;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public class EbMSAttachmentResolver extends ResourceResolverSpi
{
	@NonNull
	List<EbMSAttachment> attachments;

	@Override
	public boolean engineCanResolveURI(ResourceResolverContext context)
	{
		if (context.uriToResolve.startsWith(Constants.CID))
			return attachments.stream().anyMatch(a -> context.uriToResolve.substring(Constants.CID.length()).equals(a.getContentId()));
		return false;
	}
	
	@Override
	public XMLSignatureInput engineResolveURI(ResourceResolverContext context) throws ResourceResolverException
	{
		if (!context.uriToResolve.startsWith(Constants.CID))
			throw new ResourceResolverException(context.uriToResolve,new Object[]{"Reference URI does not start with '" + Constants.CID + "'"},context.uriToResolve,context.baseUri);
		val result = attachments.stream()
				.filter(a -> context.uriToResolve.substring(Constants.CID.length()).equals(a.getContentId()))
				.findFirst()
				.orElseThrow(() -> new ResourceResolverException(context.uriToResolve,new Object[]{"Reference URI = " + context.uriToResolve + " does not exist!"},context.uriToResolve,context.baseUri));
		try
		{
			val input = new XMLSignatureInput(result.getInputStream());
			input.setSourceURI(context.uriToResolve);
			input.setMIMEType(result.getContentType());
			return input;
		}
		catch (IOException e)
		{
			throw new ResourceResolverException(e,context.uriToResolve,context.baseUri,context.uriToResolve);
		}
	}
}
