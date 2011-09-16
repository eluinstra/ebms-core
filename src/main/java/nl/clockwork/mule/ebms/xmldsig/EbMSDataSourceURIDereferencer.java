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
package nl.clockwork.mule.ebms.xmldsig;

import java.util.List;

import javax.activation.DataSource;
import javax.xml.crypto.Data;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;

import nl.clockwork.mule.ebms.model.EbMSDataSource;

import org.apache.xml.security.signature.XMLSignatureInput;

public class EbMSDataSourceURIDereferencer implements URIDereferencer
{
	private List<EbMSDataSource> dataSources;

	public EbMSDataSourceURIDereferencer(List<EbMSDataSource> dataSources)
	{
		this.dataSources = dataSources;
	}

	@Override
	public Data dereference(URIReference uriReference, XMLCryptoContext context) throws URIReferenceException
	{
		try
		{
			if (uriReference.getURI().startsWith("cid:"))
			{
				DataSource ds = null;
				for (EbMSDataSource dataSource : dataSources)
					if (uriReference.getURI().substring("cid:".length()).equals(dataSource.getContentId()))
					{
						ds = dataSource;
						break;
					}
				if (ds == null)
					throw new URIReferenceException("Reference URI = " + uriReference.getURI() + " does not exist!");
				XMLSignatureInput in = new XMLSignatureInput(ds.getInputStream());
				if (in.isOctetStream())
					return new ApacheOctetStreamData(in);
				else
					return new ApacheNodeSetData(in);
			}
			else
				return DOMURIDereferencer.INSTANCE.dereference(uriReference,context);
		}
		catch (Exception e)
		{
			throw new URIReferenceException(e);
		}
	}

}
