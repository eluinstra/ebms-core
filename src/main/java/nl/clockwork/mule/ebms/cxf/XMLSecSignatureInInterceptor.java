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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import nl.clockwork.common.util.SecurityUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.xml.dsig.EbMSAttachmentResolver;

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
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XMLSecSignatureInInterceptor extends AbstractSoapInterceptor
{
  protected transient Log logger = LogFactory.getLog(getClass());

  private String keyStorePath;
	private String keyStorePassword;

	public XMLSecSignatureInInterceptor()
	{
		super(Phase.POST_STREAM);
		addAfter(StaxInInterceptor.class.getName());
		//FIXME move to mule init bean??? 
		org.apache.xml.security.Init.init();
	}

	private boolean verify(KeyStore keyStore, Document document, List<EbMSAttachment> attachments) throws XMLSignatureException, XMLSecurityException, CertificateExpiredException, CertificateNotYetValidException, KeyStoreException
	{
		NodeList nodeList = document.getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS,org.apache.xml.security.utils.Constants._TAG_SIGNATURE);
		if (nodeList.getLength() > 0)
		{
			XMLSignature signature = new XMLSignature((Element)nodeList.item(0),org.apache.xml.security.utils.Constants.SignatureSpecNS);
	
			EbMSAttachmentResolver resolver = new EbMSAttachmentResolver(attachments);
			signature.addResourceResolver(resolver);
	
			X509Certificate certificate = signature.getKeyInfo().getX509Certificate();
			if (certificate != null)
			{
				certificate.checkValidity();
				Enumeration<String> aliases = keyStore.aliases();
				while (aliases.hasMoreElements())
				{
					try
					{
						Certificate c = keyStore.getCertificate(aliases.nextElement());
						certificate.verify(c.getPublicKey());
						return signature.checkSignatureValue(certificate);
					}
					catch (KeyStoreException e)
					{
						throw e;
					}
					catch (Exception e)
					{
					}
				}
			}
			else
			{
				PublicKey publicKey = signature.getKeyInfo().getPublicKey();
				if (publicKey != null)
					return signature.checkSignatureValue(publicKey);
			}
			return false;
		}
		return true;
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault
	{
		try
		{
			KeyStore keyStore = SecurityUtils.loadKeyStore(keyStorePath,keyStorePassword);

			XMLStreamReader reader = message.getContent(XMLStreamReader.class);
			Document document = StaxUtils.read(reader);
			XMLStreamReader copy = StaxUtils.createXMLStreamReader(document);
			message.setContent(XMLStreamReader.class,copy);

			List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
			if (message.getAttachments() != null)
				for (Attachment attachment : message.getAttachments())
					attachments.add(new EbMSAttachment(attachment.getDataHandler().getDataSource(),attachment.getId(),attachment.getDataHandler().getName()));

			if (!verify(keyStore,document,attachments))
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

	public void setKeyStorePath(String keyStorePath)
	{
		this.keyStorePath = keyStorePath;
	}
	
	public void setKeyStorePassword(String keyStorePassword)
	{
		this.keyStorePassword = keyStorePassword;
	}
}
