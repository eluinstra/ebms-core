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
package nl.clockwork.ebms.signing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.common.JAXBParser;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.Role;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.Init;
import org.ehcache.core.Ehcache;
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

@TestInstance(value = Lifecycle.PER_CLASS)
public class SigningTest
{
	private CPAManager cpaManager;
	private EbMSMessageFactory messageFactory;
	private String cpaId = "cpaStubEBF.rm.https.signed";
	private String keyStorePath = "keystore.jks";
	private String keyStorePassword = "password";
	private EbMSSignatureGenerator signatureGenerator;
	private EbMSSignatureValidator signatureValidator;
	@Mock
	private Ehcache<String,Object> ehCacheMock;

	@BeforeAll
	public void init() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		Init.init();
		cpaManager = initCPAManager();
		messageFactory = initMessageFactory(cpaManager);
		signatureGenerator = initSignatureGenerator(cpaManager);
		signatureValidator = initSignatureValidator(cpaManager);
	}

	@Test
	public void testSiging() throws EbMSProcessorException, ValidatorException
	{
		EbMSMessage message = createMessage();
		signatureGenerator.generate(message);
		signatureValidator.validate(message);
	}

	@Test
	public void testSigingHeaderValidationFailure() throws EbMSProcessorException, ValidatorException
	{
		EbMSMessage message = createMessage();
		signatureGenerator.generate(message);
		changeConversationId(message);
		assertThrows(ValidationException.class,()->signatureValidator.validate(message));
	}

	@Test
	public void testSigingAttachmentValidationFailure() throws EbMSProcessorException, ValidatorException
	{
		EbMSMessage message = createMessage();
		signatureGenerator.generate(message);
		message.setAttachments(createAttachments(message.getMessageHeader().getMessageData().getMessageId()));
		assertThrows(ValidationException.class,()->signatureValidator.validate(message));
	}

	private void changeConversationId(EbMSMessage message)
	{
		Document d = message.getMessage();
		Node conversationId = d.getElementsByTagNameNS("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd","ConversationId").item(0);
		conversationId.setTextContent(conversationId.getTextContent() + "0");
	}

	private CPAManager initCPAManager() throws DAOException, IOException, JAXBException
	{
		CPAManager result = new CPAManager();
		result.setMethodCache(initMethodCacheMock());
		result.setEbMSDAO(initEbMSDAOMock());
		return result;
	}

	private Ehcache<String,Object> initMethodCacheMock()
	{
		Mockito.when(ehCacheMock.remove(Mockito.anyString(),Mockito.any(Serializable.class))).thenReturn(true);
		return ehCacheMock;
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
		result.setCpaManager(cpaManager);
		result.setCleoPatch(false);
		return result;
	}

	private EbMSSignatureGenerator initSignatureGenerator(CPAManager cpaManager) throws Exception
	{
		EbMSSignatureGenerator result = new EbMSSignatureGenerator();
		result.setCpaManager(cpaManager);
		result.setCanonicalizationMethodAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
		result.setTransformAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
		result.setKeyStorePath(keyStorePath);
		result.setKeyStorePassword(keyStorePassword);
		result.afterPropertiesSet();
		return result;
	}

	private EbMSSignatureValidator initSignatureValidator(CPAManager cpaManager) throws Exception
	{
		EbMSSignatureValidator result = new EbMSSignatureValidator();
		result.setCpaManager(cpaManager);
		result.setTrustStorePath(keyStorePath);
		result.setTrustStorePassword(keyStorePassword);
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
