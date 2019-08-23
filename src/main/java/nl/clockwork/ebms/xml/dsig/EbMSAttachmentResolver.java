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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;

import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverContext;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSAttachment;

public class EbMSAttachmentResolver extends ResourceResolverSpi
{
	private static final int BUFFERSIZE = 4096;
	private List<EbMSAttachment> attachments = new ArrayList<>();

	public EbMSAttachmentResolver()
	{
	}
	
	public EbMSAttachmentResolver(List<EbMSAttachment> attachments)
	{
		this.attachments = attachments;
	}

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

		DataSource result = attachments.stream()
				.filter(a -> context.uriToResolve.substring(Constants.CID.length()).equals(a.getContentId()))
				.findFirst()
				.orElseThrow(() -> new ResourceResolverException(context.uriToResolve,new Object[]{"Reference URI = " + context.uriToResolve + " does not exist!"},context.uriToResolve,context.baseUri));

		XMLSignatureInput input;
		try
		{
			final InputStream in = result.getInputStream();
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[] buffer = new byte[BUFFERSIZE];
			for (int c = in.read(buffer); c != -1; c = in.read(buffer))
				out.write(buffer,0,c);
			input = new XMLSignatureInput(out.toByteArray());
		}
		catch (IOException e)
		{
			throw new ResourceResolverException(e,context.uriToResolve,context.baseUri,context.uriToResolve);
		}
		input.setSourceURI(context.uriToResolve);
		input.setMIMEType(result.getContentType());

		return input;
	}

	public List<EbMSAttachment> getAttachments()
	{
		return attachments;
	}

}
