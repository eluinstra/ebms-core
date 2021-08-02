/**
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
package nl.clockwork.ebms.encryption;

import static nl.clockwork.ebms.cpa.CPATestUtils.loadCPA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.Init;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.EbMSIdGenerator;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.cpa.CPADAO;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.cpa.url.URLMappingDAO;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.security.KeyStoreType;
import nl.clockwork.ebms.service.model.DataSource;
import nl.clockwork.ebms.service.model.MessageRequest;
import nl.clockwork.ebms.service.model.MessageRequestProperties;
import nl.clockwork.ebms.service.model.Party;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

@TestInstance(value = Lifecycle.PER_CLASS)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EncryptionTest
{
	CPAManager cpaManager;
	EbMSMessageFactory messageFactory;
	String cpaId = "cpaStubEBF.rm.https.signed.encrypted";
	KeyStoreType keyStoreType = KeyStoreType.JKS;
	String keyStorePath = "nl/clockwork/ebms/keystore.jks";
	String keyStorePassword = "password";
	EbMSMessageEncrypter messageEncrypter;
	EbMSMessageDecrypter messageDecrypter;

	@BeforeAll
	public void init() throws Exception
	{
		MockitoAnnotations.openMocks(this);
		Init.init();
		cpaManager = initCPAManager();
		messageFactory = initMessageFactory(cpaManager);
		messageEncrypter = initMessageEncrypter(cpaManager);
		messageDecrypter = initMessageDecrypter(cpaManager);
	}

	@Test
	public void testEncryption() throws EbMSProcessorException, ValidatorException, IOException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		messageDecrypter.decrypt(message);
		assertThat(IOUtils.toString(message.getAttachments().get(0).getInputStream(),Charset.forName("UTF-8"))).isEqualTo("Dit is een test.");
	}

	@Test
	public void testEncryptionAttachmentValidationFailure() throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment(message);
		assertThatThrownBy(() -> messageDecrypter.decrypt(message)).isInstanceOf(EbMSValidationException.class);
	}

	@Test
	public void testEncryptionAttachmentValidationFailure1() throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment1(message);
		assertThatThrownBy(() -> messageDecrypter.decrypt(message)).isInstanceOf(EbMSValidationException.class);
	}

	@Test
	public void testEncryptionAttachmentNotEncrypted() throws EbMSProcessorException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		message.getAttachments().clear();
		message.getAttachments().addAll(createAttachments(message.getMessageHeader().getMessageData().getMessageId()));
		assertThatThrownBy(() -> messageDecrypter.decrypt(message)).isInstanceOf(EbMSValidationException.class);
	}

	private void changeAttachment(EbMSMessage message) throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		val attachment = message.getAttachments().get(0);
		val d = DOMUtils.read(attachment.getInputStream());
		val cipherValue = d.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#","CipherValue").item(0);
		cipherValue.setTextContent("XXXXXXX" + cipherValue.getTextContent());
		message.getAttachments().remove(0);
		message.getAttachments().add(EbMSAttachmentFactory.createEbMSAttachment(attachment.getName(),attachment.getContentId(),"application/xml",DOMUtils.toString(d).getBytes("UTF-8")));
	}

	private void changeAttachment1(EbMSMessage message) throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		val attachment = message.getAttachments().get(0);
		val d = DOMUtils.read(attachment.getInputStream());
		val cipherValue = d.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#","CipherValue").item(1);
		cipherValue.setTextContent("XXXXXXX" + cipherValue.getTextContent());
		message.getAttachments().remove(0);
		message.getAttachments().add(EbMSAttachmentFactory.createEbMSAttachment(attachment.getName(),attachment.getContentId(),"application/xml",DOMUtils.toString(d).getBytes("UTF-8")));
	}

	private CPAManager initCPAManager() throws IOException, JAXBException
	{
		return new CPAManager(initCPADAOMock(),new URLMapper(initURLMappingDAOMock()));
	}

	private CPADAO initCPADAOMock() throws IOException, JAXBException
	{
		val result = mock(CPADAO.class);
		when(result.getCPA(cpaId)).thenReturn(loadCPA(cpaId));
		return result;
	}

	private URLMappingDAO initURLMappingDAOMock()
	{
		return mock(URLMappingDAO.class);
	}

	private EbMSMessageFactory initMessageFactory(CPAManager cpaManager)
	{
		return new EbMSMessageFactory(cpaManager,new EbMSIdGenerator());
	}

	private EbMSMessageEncrypter initMessageEncrypter(CPAManager cpaManager) throws Exception
	{
		val trustStore = EbMSTrustStore.of(keyStoreType,keyStorePath,keyStorePassword);
		return new EbMSMessageEncrypter(cpaManager,trustStore);
	}

	private EbMSMessageDecrypter initMessageDecrypter(CPAManager cpaManager) throws Exception
	{
		val keyStore = EbMSKeyStore.of(keyStoreType,keyStorePath,keyStorePassword,keyStorePassword);
		return new EbMSMessageDecrypter(cpaManager,keyStore);
	}

	private EbMSMessage createMessage() throws EbMSProcessorException
	{
		val message = createMessage(cpaId);
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
				new Party("urn:osb:oin:00000000000000000000","DIGIPOORT"),
				"urn:osb:services:osb:afleveren:1.1$1.0",
				"afleveren");
	}

	private List<DataSource> createDataSources()
	{
		val result = new ArrayList<DataSource>();
		result.add(new DataSource("test.txt",null,"plain/text; charset=utf-8","Dit is een test.".getBytes(Charset.forName("UTF-8"))));
		return result;
	}

	private List<EbMSAttachment> createAttachments(String messageId)
	{
		val result = new ArrayList<EbMSAttachment>();
		result.add(EbMSAttachmentFactory.createEbMSAttachment(createContentId(messageId,1),createDataSource()));
		return result;
	}

	private javax.activation.DataSource createDataSource()
	{
		return EbMSAttachmentFactory.createEbMSAttachment("test.txt","plain/text; charset=utf-8","Dit is een andere test.".getBytes(Charset.forName("UTF-8"))); 
	}

	private String createContentId(String messageId, int i)
	{
		return messageId.replaceAll("^([^@]+)@(.+)$","$1-" + i + "@$2");
	}

}
