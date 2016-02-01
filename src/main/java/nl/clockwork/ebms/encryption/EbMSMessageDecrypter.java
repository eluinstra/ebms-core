package nl.clockwork.ebms.encryption;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EbMSMessageDecrypter
{
	public EbMSMessageDecrypter()
	{
		org.apache.xml.security.Init.init();
	}

	public void decrypt(EbMSMessage message)
	{
		try
		{
			for (EbMSAttachment attachment: message.getAttachments())
			{
				decrypt(attachment);
			}
		}
		catch (Exception e)
		{
			//FIXME
			throw new RuntimeException(e);
		}
	}

	private void decrypt(EbMSAttachment attachment) throws Exception
	{
		Document document = DOMUtils.read((attachment.getInputStream()));
		Element encryptedDataElement = (Element)document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).item(0);

		KeyStore keyStore = SecurityUtils.loadKeyStore("/home/edwin/Downloads/keystore.logius.jks","password");
		KeyPair keyPair = SecurityUtils.getKeyPair(keyStore,"1","password");
		PrivateKey private1 = keyPair.getPrivate();

		XMLCipher xmlCipher = XMLCipher.getInstance();
		xmlCipher.init(XMLCipher.DECRYPT_MODE,null);
		xmlCipher.setKEK(private1);

		xmlCipher.doFinal(document,encryptedDataElement);
		//DOMUtils.
	}

	private static Document loadEncryptionDocument() throws Exception
	{
		String fileName = "/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.xml";
		File encryptionFile = new File(fileName);
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(encryptionFile);
		System.out.println("Encryption document loaded from " + encryptionFile.toURI().toURL().toString());
		return document;
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

		//outputDocToFile(document, "/home/edwin/Downloads/A1453383414677.12095612@ebms.cv.prod.osb.overheid.nl_cn.decrypted.xml");
	}
}
