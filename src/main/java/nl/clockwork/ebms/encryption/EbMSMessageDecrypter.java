package nl.clockwork.ebms.encryption;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import javax.mail.util.ByteArrayDataSource;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EbMSMessageDecrypter implements InitializingBean
{
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

	public void decrypt(EbMSMessage message)
	{
		try
		{
			List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
			for (EbMSAttachment attachment: message.getAttachments())
				attachments.add(decrypt(attachment));
			message.setAttachments(attachments);
		}
		catch (Exception e)
		{
			//FIXME
			throw new RuntimeException(e);
		}
	}

	private EbMSAttachment decrypt(EbMSAttachment attachment) throws Exception
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
	
				KeyPair keyPair = SecurityUtils.getKeyPairByCertificateSubject(keyStore,keyName,"password");
				PrivateKey privateKey = keyPair.getPrivate();

				xmlCipher.init(XMLCipher.DECRYPT_MODE,null);
				xmlCipher.setKEK(privateKey);
		
				byte[] buffer = xmlCipher.decryptToByteArray(encryptedDataElement);
				ByteArrayDataSource ds = new ByteArrayDataSource(new ByteArrayInputStream(buffer),attachment.getContentType());
				
				return new EbMSAttachment(ds,attachment.getContentId());
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
