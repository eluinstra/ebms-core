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
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.crypto.dsig.spec.XPathFilterParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.EbMSAttachment;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLDSignatureOutInterceptor extends AbstractSoapInterceptor
{
  public static final String OUTPUT_STREAM_HOLDER = XMLDSignatureOutInterceptor.class.getName() + ".outputstream";
  protected transient Log logger = LogFactory.getLog(getClass());

  private String keyStorePath;
  private String keyStorePassword;
  private String keyAlias;
  private String keyPassword;

	public XMLDSignatureOutInterceptor()
	{
		super(Phase.WRITE);
		addBefore(SoapOutInterceptor.class.getName());
		//super(Phase.PRE_STREAM);
		//addAfter(StaxOutInterceptor.class.getName());
	}

	private String getEncoding(Message message)
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
	    message.getInterceptorChain().add(new XMLDSignatureOutEndingInterceptor()); 
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

	public class XMLDSignatureOutEndingInterceptor extends AbstractSoapInterceptor
	{
		public XMLDSignatureOutEndingInterceptor()
		{
			super(XMLDSignatureOutEndingInterceptor.class.getName(),Phase.WRITE_ENDING);
			addAfter(SoapOutInterceptor.SoapOutEndingInterceptor.class.getName());
			//super(XMLDSignatureOutEndingInterceptor.class.getName(),Phase.PRE_STREAM_ENDING);
			//addBefore(StaxOutInterceptor.StaxOutEndingInterceptor.class.getName());
		}
		
		private void sign(KeyStore keyStore, KeyPair keyPair, String alias, Document document, List<EbMSAttachment> attachments) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException, KeyException, MarshalException, XMLSignatureException, KeyStoreException
		{
			//XMLSignatureFactory signFactory = XMLSignatureFactory.getInstance("DOM");
			XMLSignatureFactory signFactory = XMLSignatureFactory.getInstance();
			DigestMethod sha1DigestMethod = signFactory.newDigestMethod(DigestMethod.SHA1,null);
	
			List<Transform> transforms = new ArrayList<Transform>();
			transforms.add(signFactory.newTransform(Transform.ENVELOPED,(TransformParameterSpec)null));
			Map<String,String> m = new HashMap<String,String>();
			m.put("soap","http://schemas.xmlsoap.org/soap/envelope/");
			transforms.add(signFactory.newTransform(Transform.XPATH,new XPathFilterParameterSpec("not(ancestor-or-self::node()[@soap:actor=\"urn:oasis:names:tc:ebxml-msg:service:nextMSH\"]|ancestor-or-self::node()[@soap:actor=\"http://schemas.xmlsoap.org/soap/actor/next\"])",m)));
			transforms.add(signFactory.newTransform(CanonicalizationMethod.INCLUSIVE,(TransformParameterSpec)null));
	
			List<Reference> references = new ArrayList<Reference>();
			references.add(signFactory.newReference("",sha1DigestMethod,transforms,null,null));
	
			for (EbMSAttachment attachment : attachments)
				references.add(signFactory.newReference("cid:" + attachment.getContentId(),sha1DigestMethod,Collections.emptyList(),null,null,DigestUtils.sha(IOUtils.toByteArray(attachment.getInputStream()))));
	
			SignedInfo signedInfo = signFactory.newSignedInfo(signFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,(C14NMethodParameterSpec)null),signFactory.newSignatureMethod(SignatureMethod.RSA_SHA1,null),references);
	
			List<XMLStructure> keyInfoElements = new ArrayList<XMLStructure>();
			KeyInfoFactory keyInfoFactory = signFactory.getKeyInfoFactory();
			keyInfoElements.add(keyInfoFactory.newKeyValue(keyPair.getPublic()));
	
			Certificate[] certificates = keyStore.getCertificateChain(alias);
			//keyInfoElements.add(keyInfoFactory.newX509Data(Arrays.asList(certificates)));
			keyInfoElements.add(keyInfoFactory.newX509Data(Collections.singletonList(certificates[0])));
	
			KeyInfo keyInfo = keyInfoFactory.newKeyInfo(keyInfoElements);
	
			XMLSignature signature = signFactory.newXMLSignature(signedInfo,keyInfo);
	
			Element soapHeader = getFirstChildElement(document.getDocumentElement());
			DOMSignContext signContext = new DOMSignContext(keyPair.getPrivate(),soapHeader);
			signContext.putNamespacePrefix(XMLSignature.XMLNS,"ds");
			signature.sign(signContext);
		}
		
		private Element getFirstChildElement(Node node)
		{
			Node child = node.getFirstChild();
			while ((child != null) && (child.getNodeType() != Node.ELEMENT_NODE))
				child = child.getNextSibling();
			return (Element)child;
		}
	
		@Override
		public void handleMessage(SoapMessage message) throws Fault
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
				
				List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
				if (message.getAttachments() != null)
					for (Attachment attachment : message.getAttachments())
					{
						DataSource ds = attachment.getDataHandler().getDataSource();
						attachments.add(new EbMSAttachment(ds,attachment.getId(),attachment.getDataHandler().getName()));
					}
	
				sign(keyStore,keyPair,keyAlias,document,attachments);
	
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
