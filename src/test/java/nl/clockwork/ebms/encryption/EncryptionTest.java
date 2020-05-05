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
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.EbMSIdGenerator;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSAttachmentFactory.DefaultEbMSAttachmentFactory;
import nl.clockwork.ebms.common.JAXBParser;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.URLMapper;
import nl.clockwork.ebms.cpa.dao.CPADAO;
import nl.clockwork.ebms.cpa.dao.URLMappingDAO;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.security.KeyStoreType;
import nl.clockwork.ebms.service.model.EbMSDataSource;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.Role;
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
	String keyStorePath = "keystore.jks";
	String keyStorePassword = "password";
	EbMSMessageEncrypter messageEncrypter;
	EbMSMessageDecrypter messageDecrypter;
	@Mock
	Ehcache ehCacheMock;

	@BeforeAll
	public void init() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		Init.init();
		EbMSAttachmentFactory.setInstance(DefaultEbMSAttachmentFactory.builder().build());
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
		assertEquals("Dit is een test.",IOUtils.toString(message.getAttachments().get(0).getInputStream(),Charset.forName("UTF-8")));
	}

	@Test
	public void testEncryptionAttachmentValidationFailure() throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment(message);
		assertThrows(EbMSValidationException.class,()->messageDecrypter.decrypt(message));
	}

	@Test
	public void testEncryptionAttachmentValidationFailure1() throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment1(message);
		assertThrows(EbMSValidationException.class,()->messageDecrypter.decrypt(message));
	}

	@Test
	public void testEncryptionAttachmentNotEncrypted() throws EbMSProcessorException, ValidatorException
	{
		val message = createMessage();
		messageEncrypter.encrypt(message);
		message.getAttachments().clear();
		message.getAttachments().addAll(createAttachments(message.getMessageHeader().getMessageData().getMessageId()));
		assertThrows(EbMSValidationException.class,()->messageDecrypter.decrypt(message));
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

	private CPAManager initCPAManager() throws DAOException, IOException, JAXBException
	{
		return new CPAManager(initMethodCacheMock(),initMethodCacheMock(),initCPADAOMock(),new URLMapper(initMethodCacheMock(),initURLMappingDAOMock()));
	}

	private Ehcache initMethodCacheMock()
	{
		val result = Mockito.mock(Ehcache.class);
		Mockito.when(result.remove(Mockito.any(Serializable.class))).thenReturn(true);
		return result;
	}

	private CPADAO initCPADAOMock() throws DAOException, IOException, JAXBException
	{
		val result = Mockito.mock(CPADAO.class);
		Mockito.when(result.getCPA(cpaId)).thenReturn(loadCPA(cpaId));
		return result;
	}

	private Optional<CollaborationProtocolAgreement> loadCPA(String cpaId) throws IOException, JAXBException
	{
		val s = IOUtils.toString(this.getClass().getResourceAsStream("/nl/clockwork/ebms/cpa/" + cpaId + ".xml"),Charset.forName("UTF-8"));
		return Optional.of(JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(s));
	}

	private URLMappingDAO initURLMappingDAOMock()
	{
		val result = Mockito.mock(URLMappingDAO.class);
		return result;
	}

	private EbMSMessageFactory initMessageFactory(CPAManager cpaManager)
	{
		return new EbMSMessageFactory(cpaManager,new EbMSIdGenerator());
	}

	private EbMSMessageEncrypter initMessageEncrypter(CPAManager cpaManager) throws Exception
	{
		val trustStore = new EbMSTrustStore(keyStoreType,keyStorePath,keyStorePassword);
		return new EbMSMessageEncrypter(cpaManager,trustStore);
	}

	private EbMSMessageDecrypter initMessageDecrypter(CPAManager cpaManager) throws Exception
	{
		val keyStore = new EbMSKeyStore(keyStoreType,keyStorePath,keyStorePassword,keyStorePassword);
		return new EbMSMessageDecrypter(cpaManager,keyStore);
	}

	private EbMSMessage createMessage() throws EbMSProcessorException
	{
		val content = createEbMSMessageContent(cpaId);
		val result = messageFactory.createEbMSMessage(content);
		return result;
	}

	private EbMSMessageContent createEbMSMessageContent(String cpaId)
	{
		val result = new EbMSMessageContent();
		result.setContext(createEbMSMessageContext(cpaId));
		result.setDataSources(createDataSources());
		return result;
	}

	private EbMSMessageContext createEbMSMessageContext(String cpaId)
	{
		val result = new EbMSMessageContext();
		result.setCpaId(cpaId);
		result.setFromRole(new Role("urn:osb:oin:00000000000000000000","DIGIPOORT"));
		result.setToRole(new Role("urn:osb:oin:00000000000000000001","OVERHEID"));
		result.setService("urn:osb:services:osb:afleveren:1.1$1.0");
		result.setAction("afleveren");
		return result;
	}

	private List<EbMSDataSource> createDataSources()
	{
		val result = new ArrayList<EbMSDataSource>();
		result.add(new EbMSDataSource("test.txt","plain/text; charset=utf-8","Dit is een test.".getBytes(Charset.forName("UTF-8"))));
		return result;
	}

	private List<EbMSAttachment> createAttachments(String messageId)
	{
		val result = new ArrayList<EbMSAttachment>();
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
