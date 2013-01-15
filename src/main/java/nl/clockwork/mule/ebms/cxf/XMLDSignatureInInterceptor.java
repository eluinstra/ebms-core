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
package nl.clockwork.mule.ebms.cxf;

import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.mule.ebms.xmldsig.EbMSDataSourceURIDereferencer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XMLDSignatureInInterceptor extends AbstractSoapInterceptor
{
  protected transient Log logger = LogFactory.getLog(getClass());

	public XMLDSignatureInInterceptor()
	{
		super(Phase.POST_STREAM);
		addAfter(StaxInInterceptor.class.getName());
	}

	private class XMLDSigKeySelector extends KeySelector
	{
		public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException
		{
			if (keyInfo == null)
				throw new KeySelectorException("KeyInfo is null!");
			for (Object xmlStructure : keyInfo.getContent())
			{
				if (xmlStructure instanceof X509Data)
				{
					final Certificate certificate = (Certificate)((X509Data)xmlStructure).getContent().get(0);
					return 
						new KeySelectorResult()
						{
							@Override
							public Key getKey()
							{
								return certificate.getPublicKey();
							}
						}
					;
				}
				if (xmlStructure instanceof KeyValue)
				{
					try
					{
						final PublicKey publicKey = ((KeyValue)xmlStructure).getPublicKey();
						return 
						new KeySelectorResult()
						{
							@Override
							public Key getKey()
							{
								return publicKey;
							}
						}
					;
					}
					catch (KeyException e)
					{
					}
				}
			}
			throw new KeySelectorException("No Public Key found!");
  	}
	}

	private boolean verify(Document document, List<EbMSDataSource> dataSources) throws MarshalException, XMLSignatureException
	{
		NodeList nodeList = document.getElementsByTagNameNS(XMLSignature.XMLNS,"Signature");
		if (nodeList.getLength() > 0)
		{
			XMLSignatureFactory signFactory = XMLSignatureFactory.getInstance();
			DOMValidateContext validateContext = new DOMValidateContext(new XMLDSigKeySelector(),nodeList.item(0));
			URIDereferencer dereferencer = new EbMSDataSourceURIDereferencer(dataSources);
			validateContext.setURIDereferencer(dereferencer);
			XMLSignature signature = signFactory.unmarshalXMLSignature(validateContext);
			return signature.validate(validateContext);
		}
		return true;
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault
	{
		try
		{
			XMLStreamReader reader = message.getContent(XMLStreamReader.class);
			Document document = StaxUtils.read(reader);
			XMLStreamReader copy = StaxUtils.createXMLStreamReader(document);
			message.setContent(XMLStreamReader.class, copy);

			List<EbMSDataSource> dataSources = new ArrayList<EbMSDataSource>();
			if (message.getAttachments() != null)
				for (Attachment attachment : message.getAttachments())
					dataSources.add(new EbMSDataSource(attachment.getDataHandler().getDataSource(),attachment.getId(),attachment.getDataHandler().getName()));

			if (!verify(document,dataSources))
				throw new SoapFault("",new QName("InvalidSignature"));
		}
		catch (SoapFault e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new Fault(e);
		}
	}

}
