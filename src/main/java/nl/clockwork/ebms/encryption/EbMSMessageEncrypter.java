package nl.clockwork.ebms.encryption;

import java.io.FileInputStream;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PublicKey;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.clockwork.ebms.common.util.SecurityUtils;

import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class EbMSMessageEncrypter
{
	private static SecretKey GenerateAESKey() throws Exception
	{
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(128);
		return keyGenerator.generateKey();
	}

	public static void main(String[] args) throws Exception
	{
		org.apache.xml.security.Init.init();

		SecretKey secretKey = GenerateAESKey();

		KeyStore keyStore = SecurityUtils.loadKeyStore("/home/edwin/Downloads/keystore.logius.jks","password");
		KeyPair keyPair = SecurityUtils.getKeyPair(keyStore,"1","password");
		PublicKey publicKey = keyPair.getPublic();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder builder = dbFactory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader("<root></root>")));

		XMLCipher keyCipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
		keyCipher.init(XMLCipher.WRAP_MODE,publicKey);
		EncryptedKey encryptedKey = keyCipher.encryptKey(document,secretKey);

		XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
		xmlCipher.init(XMLCipher.ENCRYPT_MODE,secretKey);

    EncryptedData encryptedData = xmlCipher.getEncryptedData();
    KeyInfo keyInfo = new KeyInfo(document);
    keyInfo.add(encryptedKey);
    encryptedKey.setKeyInfo(new KeyInfo(builder.parse(new InputSource(new StringReader("<KeyName>CN=52487C45.cm-4-1b.dynamic.ziggo.nl, serialNumber=00000001820029336000, O=Ordina, C=NL</KeyName>")))));
    encryptedData.setKeyInfo(keyInfo);

    EncryptedData encryptData = xmlCipher.encryptData(document,null,new FileInputStream("/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.decrypted.xml"));
    
//		IOUtils.write(XMLMessageBuilder.getInstance(EncryptedData.class).handle(encryptData),new FileOutputStream("/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.encrypted.xml"));
	}
}
