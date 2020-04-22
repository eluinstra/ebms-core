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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.activation.DataSource;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.Init;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSIdGenerator;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.common.JAXBParser;
import nl.clockwork.ebms.common.KeyStoreManager.KeyStoreType;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.Role;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

@TestInstance(value = Lifecycle.PER_CLASS)
public class EncryptionTest
{
	private CPAManager cpaManager;
	private EbMSMessageFactory messageFactory;
	private String cpaId = "cpaStubEBF.rm.https.signed.encrypted";
	private KeyStoreType keyStoreType = KeyStoreType.JKS;
	private String keyStorePath = "keystore.jks";
	private String keyStorePassword = "password";
	private EbMSMessageEncrypter messageEncrypter;
	private EbMSMessageDecrypter messageDecrypter;
	@Mock
	Ehcache ehCacheMock;

	@BeforeAll
	public void init() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		Init.init();
		cpaManager = initCPAManager();
		messageFactory = initMessageFactory(cpaManager);
		messageEncrypter = initMessageEncrypter(cpaManager);
		messageDecrypter = initMessageDecrypter(cpaManager);
	}

	@Test
	public void testEncryption() throws EbMSProcessorException, ValidatorException, IOException
	{
		EbMSMessage message = createMessage();
		messageEncrypter.encrypt(message);
		messageDecrypter.decrypt(message);
		assertEquals("Dit is een test.",IOUtils.toString(message.getAttachments().get(0).getInputStream(),Charset.forName("UTF-8")));
	}

	@Test
	public void testEncryptionAttachmentValidationFailure() throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		EbMSMessage message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment(message);
		assertThrows(EbMSValidationException.class,()->messageDecrypter.decrypt(message));
	}

	@Test
	public void testEncryptionAttachmentValidationFailure1() throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		EbMSMessage message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment1(message);
		assertThrows(EbMSValidationException.class,()->messageDecrypter.decrypt(message));
	}

	@Test
	public void testEncryptionAttachmentNotEncrypted() throws EbMSProcessorException, ValidatorException
	{
		EbMSMessage message = createMessage();
		messageEncrypter.encrypt(message);
		message.setAttachments(createAttachments(message.getMessageHeader().getMessageData().getMessageId()));
		assertThrows(EbMSValidationException.class,()->messageDecrypter.decrypt(message));
	}

	private void changeAttachment(EbMSMessage message) throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		EbMSAttachment attachment = message.getAttachments().get(0);
		Document d = DOMUtils.read(attachment.getInputStream());
		Node cipherValue = d.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#","CipherValue").item(0);
		cipherValue.setTextContent("XXXXXXX" + cipherValue.getTextContent());
		message.getAttachments().remove(0);
		message.getAttachments().add(EbMSAttachmentFactory.createEbMSAttachment(attachment.getName(),attachment.getContentId(),"application/xml",DOMUtils.toString(d).getBytes("UTF-8")));
	}

	private void changeAttachment1(EbMSMessage message) throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		EbMSAttachment attachment = message.getAttachments().get(0);
		Document d = DOMUtils.read(attachment.getInputStream());
		Node cipherValue = d.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#","CipherValue").item(1);
		cipherValue.setTextContent("XXXXXXX" + cipherValue.getTextContent());
		message.getAttachments().remove(0);
		message.getAttachments().add(EbMSAttachmentFactory.createEbMSAttachment(attachment.getName(),attachment.getContentId(),"application/xml",DOMUtils.toString(d).getBytes("UTF-8")));
	}

	private CPAManager initCPAManager() throws DAOException, IOException, JAXBException
	{
		CPAManager result = new CPAManager();
		result.setDaoMethodCache(initMethodCacheMock());
		result.setCpaMethodCache(initMethodCacheMock());
		result.setEbMSDAO(initEbMSDAOMock());
		return result;
	}

	private Ehcache initMethodCacheMock()
	{
		Ehcache result = Mockito.mock(Ehcache.class);
		Mockito.when(result.remove(Mockito.any(Serializable.class))).thenReturn(true);
		return result;
	}

	private EbMSDAO initEbMSDAOMock() throws DAOException, IOException, JAXBException
	{
		EbMSDAO result = Mockito.mock(EbMSDAO.class);
		Mockito.when(result.getCPA(cpaId)).thenReturn(loadCPA(cpaId));
		return result;
	}

	private Optional<CollaborationProtocolAgreement> loadCPA(String cpaId) throws IOException, JAXBException
	{
		String s = IOUtils.toString(this.getClass().getResourceAsStream("/nl/clockwork/ebms/cpa/" + cpaId + ".xml"),Charset.forName("UTF-8"));
		return Optional.of(JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(s));
	}

	private EbMSMessageFactory initMessageFactory(CPAManager cpaManager)
	{
		EbMSMessageFactory result = new EbMSMessageFactory();
		result.setEbMSIdGenerator(new EbMSIdGenerator());
		result.setCpaManager(cpaManager);
		return result;
	}

	private EbMSMessageEncrypter initMessageEncrypter(CPAManager cpaManager) throws Exception
	{
		EbMSMessageEncrypter result = new EbMSMessageEncrypter();
		result.setCpaManager(cpaManager);
		result.setTrustStoreType(keyStoreType);
		result.setTrustStorePath(keyStorePath);
		result.setTrustStorePassword(keyStorePassword);
		result.afterPropertiesSet();
		return result;
	}

	private EbMSMessageDecrypter initMessageDecrypter(CPAManager cpaManager) throws Exception
	{
		EbMSMessageDecrypter result = new EbMSMessageDecrypter();
		result.setCpaManager(cpaManager);
		result.setKeyStorePath(keyStorePath);
		result.setKeyStorePassword(keyStorePassword);
		result.afterPropertiesSet();
		return result;
	}

	private EbMSMessage createMessage() throws EbMSProcessorException
	{
		EbMSMessageContent content = createEbMSMessageContent(cpaId);
		EbMSMessage result = messageFactory.createEbMSMessage(cpaId,content);
		return result;
	}

	private EbMSMessageContent createEbMSMessageContent(String cpaId)
	{
		EbMSMessageContent result = new EbMSMessageContent();
		result.setContext(createEbMSMessageContext(cpaId));
		result.setDataSources(createDataSources());
		return result;
	}

	private EbMSMessageContext createEbMSMessageContext(String cpaId)
	{
		EbMSMessageContext result = new EbMSMessageContext();
		result.setCpaId(cpaId);
		result.setFromRole(new Role("urn:osb:oin:00000000000000000000","DIGIPOORT"));
		result.setToRole(new Role("urn:osb:oin:00000000000000000001","OVERHEID"));
		result.setService("urn:osb:services:osb:afleveren:1.1$1.0");
		result.setAction("afleveren");
		return result;
	}

	private List<EbMSDataSource> createDataSources()
	{
		List<EbMSDataSource> result = new ArrayList<>();
		result.add(new EbMSDataSource("test.txt","plain/text; charset=utf-8","Dit is een test.".getBytes(Charset.forName("UTF-8"))));
		return result;
	}

	private List<EbMSAttachment> createAttachments(String messageId)
	{
		List<EbMSAttachment> result = new ArrayList<>();
		result.add(EbMSAttachmentFactory.createEbMSAttachment(createContentId(messageId,1),createDataSource()));
		return result;
	}

	private DataSource createDataSource()
	{
		return EbMSAttachmentFactory.createEbMSAttachment("test.txt","plain/text; charset=utf-8","Dit is een andere test.".getBytes(Charset.forName("UTF-8"))); 
	}

	private String createContentId(String messageId, int i)
	{
		return messageId.replaceAll("^([^@]+)@(.+)$","$1-" + i + "@$2");
	}

}
