package nl.clockwork.ebms.encryption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.mail.util.ByteArrayDataSource;
import javax.xml.parsers.ParserConfigurationException;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.EncryptionConstants;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class EbMSMessageDecrypter implements InitializingBean
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private CPAManager cpaManager;
	private String keyStorePath;
	private String keyStorePassword;
	private KeyStore keyStore;

	public EbMSMessageDecrypter()
	{
		org.apache.xml.security.Init.init();
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		keyStore = SecurityUtils.loadKeyStore(keyStorePath,keyStorePassword);
	}

	public void decrypt(EbMSMessage message) throws ValidatorException
	{
		try
		{
			if (cpaManager.isConfidential(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction()))
			{
				DeliveryChannel deliveryChannel = cpaManager.getToDeliveryChannel(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getTo().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction());
				X509Certificate certificate = CPAUtils.getX509Certificate(CPAUtils.getEncryptionCertificate(deliveryChannel));
				List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
				for (EbMSAttachment attachment: message.getAttachments())
					attachments.add(decrypt(certificate,attachment));
				message.setAttachments(attachments);
			}
		}
		catch (EbMSProcessingException | SAXException | XMLSecurityException e)
		{
			throw new ValidationException(e);
		}
		catch (ParserConfigurationException | IOException | GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
	}

	private EbMSAttachment decrypt(X509Certificate certificate, EbMSAttachment attachment) throws ParserConfigurationException, SAXException, IOException, XMLSecurityException, GeneralSecurityException, EbMSProcessingException
	{
		Document document = DOMUtils.read((attachment.getInputStream()));
		if (document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).getLength() > 0)
		{
			Element encryptedDataElement = (Element)document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).item(0);
			XMLCipher xmlCipher = XMLCipher.getInstance();
			EncryptedKey encryptedKey = xmlCipher.loadEncryptedKey(encryptedDataElement);
			if (encryptedKey.getKeyInfo().containsKeyName())
			{
				String keyName = encryptedKey.getKeyInfo().itemKeyName(0).getKeyName();
				if (certificate.getSubjectDN().getName().equals(keyName))
				{
					KeyPair keyPair = SecurityUtils.getKeyPairByCertificateSubject(keyStore,keyName,"password");
					PrivateKey privateKey = keyPair.getPrivate();
	
					xmlCipher.init(XMLCipher.DECRYPT_MODE,null);
					xmlCipher.setKEK(privateKey);
			
					byte[] buffer = xmlCipher.decryptToByteArray(encryptedDataElement);
					ByteArrayDataSource ds = new ByteArrayDataSource(new ByteArrayInputStream(buffer),attachment.getContentType());
					
					return new EbMSAttachment(ds,attachment.getContentId());
				}
				else
					throw new EbMSProcessingException("KeyName " + keyName + " does match expected certificate subject " + certificate.getSubjectDN().getName() + "!");
			}
			else
				throw new EbMSProcessingException("EncryptedData of attachment " + attachment.getContentId() + " does not contain the KeyName!");
		}
		else
			throw new EbMSProcessingException("Attachment " + attachment.getContentId() + " not encrypted!");
	}

	private static Document loadEncryptionDocument() throws Exception
	{
		File encryptionFile = new File("/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.xml");
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document document = dbf.newDocumentBuilder().parse(encryptionFile);
		return document;
	}

	public static void main(String[] args) throws Exception
	{
		org.apache.xml.security.Init.init();

		Document document = loadEncryptionDocument();
		Element encryptedDataElement = (Element)document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).item(0);

		KeyStore keyStore = SecurityUtils.loadKeyStore("/home/edwin/Downloads/keystore.logius.jks","password");
		KeyPair keyPair = SecurityUtils.getKeyPair(keyStore,"1","password");
		PrivateKey privateKey = keyPair.getPrivate();

		XMLCipher xmlCipher = XMLCipher.getInstance();
		xmlCipher.init(XMLCipher.DECRYPT_MODE,null);
		xmlCipher.setKEK(privateKey);

		byte[] result = xmlCipher.decryptToByteArray(encryptedDataElement);
		
		System.out.println(new String(result));
		IOUtils.write(result,new FileOutputStream("/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.decrypted.xml"));
	}
}
