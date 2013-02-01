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
package nl.clockwork.ebms.signing;

import java.io.IOException;
import java.security.GeneralSecurityException;
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

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.EbMSAttachment;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLDSignatureGenerator implements SignatureGenerator
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private String keyStorePath;
  private String keyStorePassword;
  private String keyAlias;
  private String keyPassword;
	private KeyStore keyStore;
	private KeyPair keyPair;

	public void init() throws GeneralSecurityException, IOException
	{
		keyStore = SecurityUtils.loadKeyStore(keyStorePath,keyStorePassword);
		keyPair = SecurityUtils.getKeyPair(keyStore,keyAlias,keyPassword);
	}
	
	@Override
	public boolean generateSignature(Document document, List<EbMSAttachment> attachments) throws Exception
	{
		try
		{
			sign(keyStore,keyPair,keyAlias,document,attachments);
			return true;
		}
		catch (XMLSignatureException e)
		{
			return false;
		}
	}
	
	private void sign(KeyStore keyStore, KeyPair keyPair, String alias, Document document, List<EbMSAttachment> attachments) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException, KeyException, MarshalException, XMLSignatureException, KeyStoreException
	{
		//XMLSignatureFactory signFactory = XMLSignatureFactory.getInstance("DOM");
		XMLSignatureFactory signFactory = XMLSignatureFactory.getInstance();
		DigestMethod sha1DigestMethod = signFactory.newDigestMethod(DigestMethod.SHA1,null);

		List<Transform> transforms = new ArrayList<Transform>();
		transforms.add(signFactory.newTransform(Transform.ENVELOPED,(TransformParameterSpec)null));
		Map<String,String> m = new HashMap<String,String>();
		m.put(Constants.NAMESPACE_PREFIX_SOAP_ENVELOPE,Constants.NAMESPACE_URI_SOAP_ENVELOPE);
		transforms.add(signFactory.newTransform(Transform.XPATH,new XPathFilterParameterSpec(Constants.TRANSFORM_XPATH,m)));
		transforms.add(signFactory.newTransform(CanonicalizationMethod.INCLUSIVE,(TransformParameterSpec)null));

		List<Reference> references = new ArrayList<Reference>();
		references.add(signFactory.newReference("",sha1DigestMethod,transforms,null,null));

		for (EbMSAttachment attachment : attachments)
			references.add(signFactory.newReference(Constants.CID + attachment.getContentId(),sha1DigestMethod,Collections.emptyList(),null,null,DigestUtils.sha1(attachment.getInputStream())));

		SignedInfo signedInfo = signFactory.newSignedInfo(signFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,(C14NMethodParameterSpec)null),signFactory.newSignatureMethod(SignatureMethod.RSA_SHA1,null),references);

		List<XMLStructure> keyInfoElements = new ArrayList<XMLStructure>();
		KeyInfoFactory keyInfoFactory = signFactory.getKeyInfoFactory();
		keyInfoElements.add(keyInfoFactory.newKeyValue(keyPair.getPublic()));

		Certificate[] certificates = keyStore.getCertificateChain(alias);
		//keyInfoElements.add(keyInfoFactory.newX509Data(Arrays.asList(certificates)));
		keyInfoElements.add(keyInfoFactory.newX509Data(Collections.singletonList(certificates[0])));

		KeyInfo keyInfo = keyInfoFactory.newKeyInfo(keyInfoElements);

		XMLSignature signature = signFactory.newXMLSignature(signedInfo,keyInfo);

		Element soapHeader = DOMUtils.getFirstChildElement(document.getDocumentElement());
		DOMSignContext signContext = new DOMSignContext(keyPair.getPrivate(),soapHeader);
		signContext.putNamespacePrefix(XMLSignature.XMLNS,Constants.NAMESPACE_PREFIX_DS);
		signature.sign(signContext);
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

}
