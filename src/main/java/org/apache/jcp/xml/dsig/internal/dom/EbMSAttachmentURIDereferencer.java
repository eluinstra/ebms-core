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
package org.apache.jcp.xml.dsig.internal.dom;

import java.io.IOException;
import java.util.List;

import javax.xml.crypto.Data;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;

import org.apache.xml.security.signature.XMLSignatureInput;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSAttachment;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSAttachmentURIDereferencer implements URIDereferencer
{
	@NonNull
	List<EbMSAttachment> attachments;

	@Override
	public Data dereference(URIReference uriReference, XMLCryptoContext context) throws URIReferenceException
	{
		try
		{
			if (uriReference.getURI().startsWith(Constants.CID))
			{
				val attachment = attachments.stream()
						.filter(a -> uriReference.getURI().substring(Constants.CID.length()).equals(a.getContentId()))
						.findFirst()
						.orElseThrow(() -> new URIReferenceException("Reference URI = " + uriReference.getURI() + " does not exist!"));
				val in = new XMLSignatureInput(attachment.getInputStream());
				if (in.isOctetStream())
					return new ApacheOctetStreamData(in);
				else
					return new ApacheNodeSetData(in);
			}
			else
				return DOMURIDereferencer.INSTANCE.dereference(uriReference,context);
		}
		catch (IOException e)
		{
			throw new URIReferenceException(e);
		}
	}

}
