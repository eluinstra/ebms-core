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
package nl.clockwork.ebms.xml.dsig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSAttachment;

import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;
import org.w3c.dom.Attr;

public class EbMSAttachmentResolver extends ResourceResolverSpi
{
	private List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();

	public EbMSAttachmentResolver()
	{
	}
	
	public EbMSAttachmentResolver(List<EbMSAttachment> attachments)
	{
		this.attachments = attachments;
	}

	@Override
	public boolean engineCanResolve(Attr uri, String baseUri)
	{
		String href = uri.getNodeValue();
		if (href.startsWith(Constants.CID))
			for (EbMSAttachment attachment: attachments)
				if (href.substring(Constants.CID.length()).equals(attachment.getContentId()))
					return true;
		return false;
	}

	@Override
	public XMLSignatureInput engineResolve(Attr uri, String baseUri) throws ResourceResolverException
	{
		String href = uri.getNodeValue();

		if (!href.startsWith(Constants.CID))
			throw new ResourceResolverException(href,new Object[]{"Reference URI does not start with '" + Constants.CID + "'"},uri,baseUri);

		DataSource result = null;
		for (EbMSAttachment attachment : attachments)
			if (href.substring(Constants.CID.length()).equals(attachment.getContentId()))
			{
				result = attachment;
				break;
			}

		if (result == null)
			throw new ResourceResolverException(href,new Object[]{"Reference URI = " + href + " does not exist!"},uri,baseUri);

		XMLSignatureInput input;
		try
		{
			final InputStream in = result.getInputStream();
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[] buffer = new byte[4096];
			for (int c = in.read(buffer); c != -1; c = in.read(buffer))
				out.write(buffer,0,c);
			input = new XMLSignatureInput(out.toByteArray());
		}
		catch (IOException e)
		{
			throw new ResourceResolverException(href,e,uri,baseUri);
		}
		input.setSourceURI(href);
		input.setMIMEType(result.getContentType());

		return input;
	}

	public List<EbMSAttachment> getAttachments()
	{
		return attachments;
	}
	
}
