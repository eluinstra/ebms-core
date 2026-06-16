/*
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
 */
package nl.clockwork.ebms.common.security;

import static nl.clockwork.ebms.api.cpa.CPATestUtils.cpaCache;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.api.ebms.model.DataSource;
import nl.clockwork.ebms.api.ebms.model.MessageRequest;
import nl.clockwork.ebms.api.ebms.model.MessageRequestProperties;
import nl.clockwork.ebms.api.ebms.model.Party;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPARepository;
import nl.clockwork.ebms.common.message.EbMSAttachmentFactory;
import nl.clockwork.ebms.common.message.EbMSIdGenerator;
import nl.clockwork.ebms.common.message.EbMSMessageFactory;
import nl.clockwork.ebms.common.model.EbMSAttachment;
import nl.clockwork.ebms.common.model.EbMSMessage;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.EbMSValidationException;
import nl.clockwork.ebms.common.util.ValidatorException;
import nl.clockwork.ebms.server.processing.EbMSProcessorException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.KeyName;
import org.apache.xml.security.utils.EncryptionConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@TestInstance(value = Lifecycle.PER_CLASS)
@FieldDefaults(level = AccessLevel.PRIVATE)
class EncryptionTest
{
	CPAManager cpaManager;
	EbMSMessageFactory messageFactory;
	String cpaId = "cpaStubEBF.rm.https.signed.encrypted";
	KeyStoreType keyStoreType = KeyStoreType.JKS;
	String keyStorePath = "nl/clockwork/ebms/keystore.jks";
	String keyStorePassword = "my-secret-password";
	EbMSMessageEncrypter messageEncrypter;
	EbMSMessageDecrypter messageDecrypter;

	@BeforeAll
	void init()
	{
		MockitoAnnotations.openMocks(this);
		Init.init();
		cpaManager = initCPAManager();
		messageFactory = initMessageFactory(cpaManager);
		messageEncrypter = initMessageEncrypter(cpaManager);
		messageDecrypter = initMessageDecrypter(cpaManager);
	}

	@Test
	void testEncryption() throws EbMSProcessorException, ValidatorException, IOException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		messageDecrypter.decrypt(message);
		assertThat(IOUtils.toString(message.getAttachments().get(0).getInputStream(), Charset.forName("UTF-8"))).isEqualTo("Dit is een test.");
	}

	@Test
	void testEncryptionAttachmentValidationFailure()
			throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment(message);
		assertThatThrownBy(() -> messageDecrypter.decrypt(message)).isInstanceOf(EbMSValidationException.class);
	}

	@Test
	void testEncryptionAttachmentValidationFailure1()
			throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment1(message);
		assertThatThrownBy(() -> messageDecrypter.decrypt(message)).isInstanceOf(EbMSValidationException.class);
	}

	@Test
	void testEncryptionAttachmentNotEncrypted() throws EbMSProcessorException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		message.getAttachments().clear();
		message.getAttachments().addAll(createAttachments(message.getMessageHeader().getMessageData().getMessageId()));
		assertThatThrownBy(() -> messageDecrypter.decrypt(message)).isInstanceOf(EbMSValidationException.class);
	}

	@Test
	void testEncryptionLargeAttachmentRoundTrip() throws EbMSProcessorException, ValidatorException, IOException
	{
		val payload = new byte[4 * 1024 * 1024];
		new Random(0xC0FFEE).nextBytes(payload);
		val message = createMessage(payload);
		messageEncrypter.encrypt(message);
		messageDecrypter.decrypt(message);
		try (val decrypted = message.getAttachments().get(0).getInputStream())
		{
			assertThat(IOUtils.contentEquals(decrypted, new ByteArrayInputStream(payload))).isTrue();
		}
	}

	@Test
	void testEncryptionEmptyAttachmentRoundTrip() throws EbMSProcessorException, ValidatorException, IOException
	{
		val message = createMessage(new byte[0]);
		messageEncrypter.encrypt(message);
		messageDecrypter.decrypt(message);
		try (val decrypted = message.getAttachments().get(0).getInputStream())
		{
			assertThat(IOUtils.toByteArray(decrypted)).isEmpty();
		}
	}

	@Test
	void testStreamingEncrypterOutputIsSantuarioCompatible() throws Exception
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		val attachment = message.getAttachments().get(0);
		val plaintext = decryptWithStockSantuario(attachment);
		assertThat(new String(plaintext, Charset.forName("UTF-8"))).isEqualTo("Dit is een test.");
	}

	@Test
	void testStreamingDecrypterAcceptsSantuarioOutput() throws Exception
	{
		val message = createMessage();
		replaceWithSantuarioEncryptedAttachment(message, "Dit is een test.".getBytes(Charset.forName("UTF-8")));
		messageDecrypter.decrypt(message);
		assertThat(IOUtils.toString(message.getAttachments().get(0).getInputStream(), Charset.forName("UTF-8"))).isEqualTo("Dit is een test.");
	}

	private byte[] decryptWithStockSantuario(EbMSAttachment attachment) throws Exception
	{
		try (InputStream in = attachment.getInputStream())
		{
			val doc = DOMUtils.read(in);
			val ed = (Element)doc.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_ENCRYPTEDDATA).item(0);
			val xc = XMLCipher.getInstance();
			xc.init(XMLCipher.DECRYPT_MODE, null);
			xc.setKEK(loadPrivateKey());
			return xc.decryptToByteArray(ed);
		}
	}

	private void replaceWithSantuarioEncryptedAttachment(EbMSMessage message, byte[] plaintext) throws Exception
	{
		val attachment = message.getAttachments().get(0);
		val certificate = loadCertificate();
		val algorithm = "http://www.w3.org/2001/04/xmlenc#aes256-cbc";
		val secretKey = nl.clockwork.ebms.common.util.SecurityUtils.generateKey(algorithm);

		val document = DOMUtils.getDocumentBuilder().newDocument();
		document.appendChild(document.createElement("root"));

		val xmlCipher = XMLCipher.getInstance(algorithm);
		xmlCipher.init(XMLCipher.ENCRYPT_MODE, secretKey);

		val keyCipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
		keyCipher.init(XMLCipher.WRAP_MODE, certificate.getPublicKey());
		val encryptedKey = keyCipher.encryptKey(document, secretKey);
		val encryptedKeyInfo = new KeyInfo(document);
		encryptedKeyInfo.add(new KeyName(document, certificate.getSubjectX500Principal().getName()));
		encryptedKey.setKeyInfo(encryptedKeyInfo);

		val encryptedData = xmlCipher.getEncryptedData();
		val keyInfo = new KeyInfo(document);
		keyInfo.add(encryptedKey);
		encryptedData.setKeyInfo(keyInfo);
		encryptedData.setId(attachment.getContentId());
		encryptedData.setMimeType(attachment.getContentType());
		encryptedData.setType(EncryptionConstants.TYPE_ELEMENT);

		val cipherResult = xmlCipher.encryptData(document, null, new ByteArrayInputStream(plaintext));
		val baos = new ByteArrayOutputStream();
		DOMUtils.getTransformer().transform(new DOMSource(xmlCipher.martial(document, cipherResult)), new StreamResult(baos));

		message.getAttachments().clear();
		message.getAttachments()
				.add(EbMSAttachmentFactory.createEbMSAttachment(attachment.getName(), attachment.getContentId(), "application/xml", baos.toByteArray()));
	}

	private X509Certificate loadCertificate() throws Exception
	{
		return (X509Certificate)loadKeyStore().getCertificate("localhost");
	}

	private PrivateKey loadPrivateKey() throws Exception
	{
		return (PrivateKey)loadKeyStore().getKey("localhost", keyStorePassword.toCharArray());
	}

	private KeyStore loadKeyStore() throws Exception
	{
		val ks = KeyStore.getInstance("JKS");
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(keyStorePath))
		{
			ks.load(in, keyStorePassword.toCharArray());
		}
		return ks;
	}

	private void changeAttachment(EbMSMessage message) throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		val attachment = message.getAttachments().get(0);
		val d = DOMUtils.read(attachment.getInputStream());
		val cipherValue = d.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#", "CipherValue").item(0);
		cipherValue.setTextContent("XXXXXXX" + cipherValue.getTextContent());
		message.getAttachments().remove(0);
		message.getAttachments()
				.add(
						EbMSAttachmentFactory
								.createEbMSAttachment(attachment.getName(), attachment.getContentId(), "application/xml", DOMUtils.toString(d).getBytes("UTF-8")));
	}

	private void changeAttachment1(EbMSMessage message) throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		val attachment = message.getAttachments().get(0);
		val d = DOMUtils.read(attachment.getInputStream());
		val cipherValue = d.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#", "CipherValue").item(1);
		cipherValue.setTextContent("XXXXXXX" + cipherValue.getTextContent());
		message.getAttachments().remove(0);
		message.getAttachments()
				.add(
						EbMSAttachmentFactory
								.createEbMSAttachment(attachment.getName(), attachment.getContentId(), "application/xml", DOMUtils.toString(d).getBytes("UTF-8")));
	}

	private CPAManager initCPAManager()
	{
		return new CPAManager(
				initCpaRepository(),
				(ignoredCpaId, certificate) -> certificate,
				url -> url,
				EbMSKeyStore.of(KeyStoreType.PKCS12, "nl/clockwork/ebms/keystore.p12", "my-secret-password", "my-secret-password"),
				false);
	}

	private CPARepository initCpaRepository()
	{
		val result = mock(CPARepository.class);
		when(result.getCPA(cpaId)).thenReturn(cpaCache.apply(cpaId));
		return result;
	}

	private EbMSMessageFactory initMessageFactory(CPAManager cpaManager)
	{
		return new EbMSMessageFactory(cpaManager, new EbMSIdGenerator());
	}

	private EbMSMessageEncrypter initMessageEncrypter(CPAManager cpaManager)
	{
		val trustStore = EbMSTrustStore.of(keyStoreType, keyStorePath, keyStorePassword);
		return new EbMSMessageEncrypter(cpaManager, trustStore);
	}

	private EbMSMessageDecrypter initMessageDecrypter(CPAManager cpaManager)
	{
		val keyStore = EbMSKeyStore.of(keyStoreType, keyStorePath, keyStorePassword, keyStorePassword);
		return new EbMSMessageDecrypter(cpaManager, keyStore);
	}

	private EbMSMessage createMessage() throws EbMSProcessorException
	{
		val message = createMessage(cpaId);
		return messageFactory.createEbMSMessage(message);
	}

	private EbMSMessage createMessage(byte[] payload) throws EbMSProcessorException
	{
		val message = createMessage(cpaId);
		message.getDataSources().clear();
		message.getDataSources().add(new DataSource("payload.bin", null, "application/octet-stream", payload));
		return messageFactory.createEbMSMessage(message);
	}

	private MessageRequest createMessage(String cpaId)
	{
		val result = new MessageRequest();
		result.setProperties(createMessageProperties(cpaId));
		result.setDataSources(createDataSources());
		return result;
	}

	private MessageRequestProperties createMessageProperties(String cpaId)
	{
		return new MessageRequestProperties(
				cpaId,
				new Party("urn:osb:oin:00000000000000000000", "DIGIPOORT"),
				"urn:osb:services:osb:afleveren:1.1$1.0",
				"afleveren");
	}

	private List<DataSource> createDataSources()
	{
		val result = new ArrayList<DataSource>();
		result.add(new DataSource("test.txt", null, "plain/text; charset=utf-8", "Dit is een test.".getBytes(Charset.forName("UTF-8"))));
		return result;
	}

	private List<EbMSAttachment> createAttachments(String messageId)
	{
		val result = new ArrayList<EbMSAttachment>();
		result.add(EbMSAttachmentFactory.createEbMSAttachment(createContentId(messageId, 1), createDataSource()));
		return result;
	}

	private jakarta.activation.DataSource createDataSource()
	{
		return EbMSAttachmentFactory.createEbMSAttachment("test.txt", "plain/text; charset=utf-8", "Dit is een andere test.".getBytes(Charset.forName("UTF-8")));
	}

	private String createContentId(String messageId, int i)
	{
		return messageId.replaceAll("^([^@]+)@(.+)$", "$1-" + i + "@$2");
	}

}
