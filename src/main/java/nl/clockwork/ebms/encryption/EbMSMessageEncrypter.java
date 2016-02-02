package nl.clockwork.ebms.encryption;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PublicKey;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.clockwork.ebms.common.util.SecurityUtils;

import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.KeyName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class EbMSMessageEncrypter
{
	private static SecretKey GenerateAESKey() throws Exception
	{
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(128);
		return keyGenerator.generateKey();
	}

	private static void outputDocToFile(Document doc, String fileName) throws Exception
	{
		File encryptionFile = new File(fileName);
		FileOutputStream f = new FileOutputStream(encryptionFile);

		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(f);
		transformer.transform(source,result);

		f.close();
		System.out.println("Wrote document containing decrypted data to " + encryptionFile.toURI().toURL().toString());
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
		KeyInfo encryptedKeyInfo = new KeyInfo(document);
		encryptedKeyInfo.add(new KeyName(document,"CN=52487C45.cm-4-1b.dynamic.ziggo.nl, serialNumber=00000001820029336000, O=Ordina, C=NL"));
		encryptedKey.setKeyInfo(encryptedKeyInfo);
		KeyInfo keyInfo = new KeyInfo(document);
		keyInfo.add(encryptedKey);
		encryptedData.setKeyInfo(keyInfo);

		//xmlCipher.doFinal(document,document.getDocumentElement(),true);

		//outputDocToFile(document,"/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.encrypted.2.xml");

		encryptedData = xmlCipher.encryptData(null,null,new FileInputStream("/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.decrypted.xml"));
		Element element = xmlCipher.martial(document,encryptedData);
		//System.out.println(DOMUtils.toString((Document)element));

		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();
		StringWriter buffer = new StringWriter();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(element),new StreamResult(buffer));
		System.out.println(buffer.toString());
		

		// IOUtils.write(XMLMessageBuilder.getInstance(EncryptedData.class).handle(encryptData),new FileOutputStream("/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.encrypted.xml"));
	}
}
