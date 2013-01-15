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

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.mule.ebms.util.SecurityUtils;
import nl.clockwork.mule.ebms.xmldsig.EbMSDataSourceResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLSecSignatureOutInterceptor extends AbstractSoapInterceptor
{
  public static final String OUTPUT_STREAM_HOLDER = XMLSecSignatureOutInterceptor.class.getName() + ".outputstream";
	protected transient Log logger = LogFactory.getLog(getClass());

	private String keyStorePath;
	private String keyStorePassword;
	private String keyAlias;
	private String keyPassword;

	public XMLSecSignatureOutInterceptor()
	{
		this(Phase.WRITE);
		addBefore(SoapOutInterceptor.class.getName());
		//super(Phase.PRE_STREAM);
		//addAfter(StaxOutInterceptor.class.getName());
		//FIXME move to mule init bean??? 
		org.apache.xml.security.Init.init();
	}
	
	public XMLSecSignatureOutInterceptor(String phase)
	{
		super(phase);
	}
	
	protected String getEncoding(Message message)
	{
		Exchange ex = message.getExchange();
		String encoding = (String)message.get(Message.ENCODING);
		if (encoding == null && ex.getInMessage() != null)
		{
			encoding = (String) ex.getInMessage().get(Message.ENCODING);
			message.put(Message.ENCODING, encoding);
		}

		if (encoding == null)
		{
			encoding = "UTF-8";
			message.put(Message.ENCODING, encoding);
		}
		return encoding;
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault
	{
    try
		{
    		OutputStream originalOs = message.getContent(OutputStream.class);
			message.put(OUTPUT_STREAM_HOLDER,originalOs);
			CachedOutputStream cos = new CachedOutputStream();
			message.setContent(OutputStream.class,cos);
			message.setContent(XMLStreamWriter.class,StaxOutInterceptor.getXMLOutputFactory(message).createXMLStreamWriter(cos,getEncoding(message)));
			message.getInterceptorChain().add(new XMLSecSignatureOutEndingInterceptor()); 
		}
		catch (XMLStreamException e)
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

	public void setKeyAlias(String keyAlias)
	{
		this.keyAlias = keyAlias;
	}
	
	public void setKeyPassword(String keyPassword)
	{
		this.keyPassword = keyPassword;
	}

	public class XMLSecSignatureOutEndingInterceptor extends AbstractSoapInterceptor
	{
		private static final String NAMESPACE_URI_XML_NS = "http://www.w3.org/2000/xmlns/";
		private static final String NAMESPACE_URI_SOAP_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
		private static final String TRANSFORM_ALGORITHM_XPATH = "http://www.w3.org/TR/1999/REC-xpath-19991116";
		private static final String NAMESPACE_PREFIX_SOAP_ENVELOPE = "soap";
		//private static final String TRANSFORM_XPATH = "not(ancestor-or-self::node()[@" + NAMESPACE_PREFIX_SOAP_ENVELOPE + ":actor=\"" + ACTOR_NEXT_MSH_URN + "\"] | ancestor-or-self::node()[@" + NAMESPACE_PREFIX_SOAP_ENVELOPE + ":actor=\"" + ACTOR_NEXT_MSH_SCHEMAS + "\"])";
		private static final String TRANSFORM_XPATH = "not(ancestor-or-self::node()[@" + NAMESPACE_PREFIX_SOAP_ENVELOPE + ":actor=\"urn:oasis:names:tc:ebxml-msg:service:nextMSH\"]|ancestor-or-self::node()[@" + NAMESPACE_PREFIX_SOAP_ENVELOPE + ":actor=\"http://schemas.xmlsoap.org/soap/actor/next\"])";
		private static final String NAMESPACE_PREFIX_DS = "ds";
	
		public XMLSecSignatureOutEndingInterceptor()
		{
			this(XMLSecSignatureOutEndingInterceptor.class.getName(),Phase.WRITE_ENDING);
			addAfter(SoapOutInterceptor.SoapOutEndingInterceptor.class.getName());
			//super(XMLsecSignatureOutEndingInterceptor.class.getName(),Phase.PRE_STREAM_ENDING);
			//addBefore(StaxOutInterceptor.StaxOutEndingInterceptor.class.getName());
		}
	
		public XMLSecSignatureOutEndingInterceptor(String clazz, String phase)
		{
			super(clazz,phase);
		}
		
		private void sign(KeyStore keyStore, KeyPair keyPair, String alias, Document document, List<EbMSDataSource> dataSources) throws XMLSecurityException, KeyStoreException
		{
			XMLSignature signature = new XMLSignature(document,org.apache.xml.security.utils.Constants.SignatureSpecNS,XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);
	
			Element soapHeader = getFirstChildElement(document.getDocumentElement());
			soapHeader.appendChild(signature.getElement());
			
			EbMSDataSourceResolver resolver = new EbMSDataSourceResolver(dataSources);
			signature.getSignedInfo().addResourceResolver(resolver);
				
			Transforms transforms = new Transforms(document);
			transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
			Element xpath = document.createElementNS(org.apache.xml.security.utils.Constants.SignatureSpecNS,org.apache.xml.security.utils.Constants._TAG_XPATH);
			xpath.setAttributeNS(NAMESPACE_URI_XML_NS, "xmlns:" + NAMESPACE_PREFIX_SOAP_ENVELOPE,NAMESPACE_URI_SOAP_ENVELOPE);
			xpath.appendChild(document.createTextNode(TRANSFORM_XPATH));
			xpath.setPrefix(NAMESPACE_PREFIX_DS);
			transforms.addTransform(TRANSFORM_ALGORITHM_XPATH,xpath);
			transforms.addTransform(Transforms.TRANSFORM_C14N_OMIT_COMMENTS);
			
			signature.addDocument("",transforms,org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1);
			
			for (EbMSDataSource dataSource : dataSources)
				signature.addDocument("cid:" + dataSource.getContentId());
			
			signature.addKeyInfo(keyPair.getPublic());
			
			Certificate[] certificates = keyStore.getCertificateChain(alias);
		    //for (Certificate certificate : certificates)
		    //	signature.addKeyInfo((X509Certificate)certificate);
			signature.addKeyInfo((X509Certificate)certificates[0]);
	
			signature.sign(keyPair.getPrivate());
		}
		
		private Element getFirstChildElement(Node node)
		{
			Node child = node.getFirstChild();
			while ((child != null) && (child.getNodeType() != Node.ELEMENT_NODE))
				child = child.getNextSibling();
			return (Element)child;
		}
	
		@Override
		public void handleMessage(final SoapMessage message) throws Fault
		{
			try
			{
				KeyStore keyStore = SecurityUtils.loadKeyStore(keyStorePath,keyStorePassword);
				KeyPair keyPair = SecurityUtils.getKeyPair(keyStore,keyAlias,keyPassword);
	
				CachedOutputStream os = (CachedOutputStream)message.getContent(OutputStream.class);
				StringBuilder sb = new StringBuilder();
				os.writeCacheTo(sb);
				
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				Document document = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(sb.toString().getBytes()));
				
				List<EbMSDataSource> dataSources = new ArrayList<EbMSDataSource>();
				if (message.getAttachments() != null)
					for (Attachment attachment : message.getAttachments())
					{
						DataSource ds = attachment.getDataHandler().getDataSource();
						dataSources.add(new EbMSDataSource(ds,attachment.getId(),attachment.getDataHandler().getName()));
					}
	
				sign(keyStore,keyPair,keyAlias,document,dataSources);
	
				OutputStream originalOs = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.transform(new DOMSource(document),new StreamResult(originalOs));

				message.setContent(OutputStream.class,originalOs);
			}
			catch (Exception e)
			{
				throw new Fault(e);
			}
		}
	
	}

}
