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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.common.XMLMessageBuilder;
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

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.Init;
import org.junit.Test;
import org.mockito.Mockito;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class EncryptionTest
{
	private CPAManager cpaManager;
	private EbMSMessageFactory messageFactory;
	private String cpaId = "cpaStubEBF.rm.https.signed.encrypted";
	private String keyStorePath = "keystore.jks";
	private String keyStorePassword = "password";
	private EbMSMessageEncrypter messageEncrypter;
	private EbMSMessageDecrypter messageDecrypter;

	public EncryptionTest() throws Exception
	{
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
		assertEquals("Dit is een test.",IOUtils.toString(message.getAttachments().get(0).getInputStream()));
	}

	@Test(expected = EbMSValidationException.class)
	public void testEncryptionAttachmentValidationFailure() throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		EbMSMessage message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment(message);
		messageDecrypter.decrypt(message);
	}

	@Test(expected = EbMSValidationException.class)
	public void testEncryptionAttachmentValidationFailure1() throws EbMSProcessorException, ParserConfigurationException, SAXException, IOException, TransformerException, ValidatorException
	{
		EbMSMessage message = createMessage();
		messageEncrypter.encrypt(message);
		changeAttachment1(message);
		messageDecrypter.decrypt(message);
	}

	@Test(expected = EbMSValidationException.class)
	public void testEncryptionAttachmentNotEncrypted() throws EbMSProcessorException, ValidatorException
	{
		EbMSMessage message = createMessage();
		messageEncrypter.encrypt(message);
		message.setAttachments(createAttachments(message.getMessageHeader().getMessageData().getMessageId()));
		messageDecrypter.decrypt(message);
	}

	private void changeAttachment(EbMSMessage message) throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		EbMSAttachment attachment = message.getAttachments().get(0);
		Document d = DOMUtils.read(attachment.getInputStream());
		Node cipherValue = d.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#","CipherValue").item(0);
		cipherValue.setTextContent("XXXXXXX" + cipherValue.getTextContent());
		ByteArrayDataSource ds = new ByteArrayDataSource(DOMUtils.toString(d).getBytes("UTF-8"),"application/xml");
		ds.setName(attachment.getName());
		message.getAttachments().remove(0);
		message.getAttachments().add(new EbMSAttachment(ds,attachment.getContentId()));
	}

	private void changeAttachment1(EbMSMessage message) throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		EbMSAttachment attachment = message.getAttachments().get(0);
		Document d = DOMUtils.read(attachment.getInputStream());
		Node cipherValue = d.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#","CipherValue").item(1);
		cipherValue.setTextContent("XXXXXXX" + cipherValue.getTextContent());
		ByteArrayDataSource ds = new ByteArrayDataSource(DOMUtils.toString(d).getBytes("UTF-8"),"application/xml");
		ds.setName(attachment.getName());
		message.getAttachments().remove(0);
		message.getAttachments().add(new EbMSAttachment(ds,attachment.getContentId()));
	}

	private CPAManager initCPAManager() throws DAOException, IOException, JAXBException
	{
		CPAManager result = new CPAManager();
		result.setMethodCache(initMethodCacheMock());
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

	private CollaborationProtocolAgreement loadCPA(String cpaId) throws IOException, JAXBException
	{
		String s = IOUtils.toString(this.getClass().getResourceAsStream("/nl/clockwork/ebms/cpa/" + cpaId + ".xml"));
		return XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(s);
	}

	private EbMSMessageFactory initMessageFactory(CPAManager cpaManager)
	{
		EbMSMessageFactory result = new EbMSMessageFactory();
		result.setCpaManager(cpaManager);
		result.setCleoPatch(false);
		return result;
	}

	private EbMSMessageEncrypter initMessageEncrypter(CPAManager cpaManager) throws Exception
	{
		EbMSMessageEncrypter result = new EbMSMessageEncrypter();
		result.setCpaManager(cpaManager);
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
		List<EbMSDataSource> result = new ArrayList<EbMSDataSource>();
		result.add(new EbMSDataSource("test.txt",null,"plain/text; charset=utf-8","Dit is een test.".getBytes(Charset.forName("UTF-8"))));
		return result;
	}

	private List<EbMSAttachment> createAttachments(String messageId)
	{
		List<EbMSAttachment> result = new ArrayList<EbMSAttachment>();
		result.add(new EbMSAttachment(createDataSource(),createContentId(messageId,1)));
		return result;
	}

	private DataSource createDataSource()
	{
		ByteArrayDataSource result = new ByteArrayDataSource("Dit is een andere test.".getBytes(Charset.forName("UTF-8")),"plain/text; charset=utf-8");
		result.setName("test.txt");
		return result;
	}

	private String createContentId(String messageId, int i)
	{
		return messageId.replaceAll("^([^@]+)@(.+)$","$1-" + i + "@$2");
	}

}
