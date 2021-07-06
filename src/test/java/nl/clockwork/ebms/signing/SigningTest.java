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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.Init;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.EbMSIdGenerator;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPADAO;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.cpa.url.URLMappingDAO;
import nl.clockwork.ebms.jaxb.JAXBParser;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.security.KeyStoreType;
import nl.clockwork.ebms.service.model.DataSource;
import nl.clockwork.ebms.service.model.MessageRequest;
import nl.clockwork.ebms.service.model.MessageRequestProperties;
import nl.clockwork.ebms.service.model.Party;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

@TestInstance(value = Lifecycle.PER_CLASS)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SigningTest
{
	CPAManager cpaManager;
	EbMSMessageFactory messageFactory;
	String cpaId = "cpaStubEBF.rm.https.signed";
	KeyStoreType keyStoreType = KeyStoreType.JKS;
	String keyStorePath = "nl/clockwork/ebms/keystore.jks";
	String keyStorePassword = "password";
	EbMSSignatureGenerator signatureGenerator;
	EbMSSignatureValidator signatureValidator;

	@BeforeAll
	public void init() throws Exception
	{
		MockitoAnnotations.openMocks(this);
		Init.init();
		cpaManager = initCPAManager();
		messageFactory = initMessageFactory(cpaManager);
		signatureGenerator = initSignatureGenerator(cpaManager);
		signatureValidator = initSignatureValidator(cpaManager);
	}

	@Test
	public void testSiging() throws EbMSProcessorException, ValidatorException, SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val message = createMessage();
		val document = EbMSMessageUtils.getEbMSDocument(message);
		signatureGenerator.generate(document,message);
		signatureValidator.validate(document,message);
	}

	@Test
	public void testSigingHeaderValidationFailure() throws EbMSProcessorException, ValidatorException, SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val message = createMessage();
		val document = EbMSMessageUtils.getEbMSDocument(message);
		signatureGenerator.generate(document,message);
		changeConversationId(document);
		assertThrows(ValidationException.class,() -> signatureValidator.validate(document,message));
	}

	@Test
	public void testSigingAttachmentValidationFailure() throws EbMSProcessorException, ValidatorException, SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val message = createMessage();
		val document = EbMSMessageUtils.getEbMSDocument(message);
		signatureGenerator.generate(document,message);
		message.getAttachments().clear();
		message.getAttachments().addAll(createAttachments(message.getMessageHeader().getMessageData().getMessageId()));
		assertThrows(ValidationException.class,() -> signatureValidator.validate(document,message));
	}

	private void changeConversationId(EbMSDocument message)
	{
		val d = message.getMessage();
		val conversationId = d.getElementsByTagNameNS("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd","ConversationId").item(0);
		conversationId.setTextContent(conversationId.getTextContent() + "0");
	}

	private CPAManager initCPAManager() throws IOException, JAXBException
	{
		return new CPAManager(initCPADAOMock(),new URLMapper(initURLMappingDAOMock()));
	}

	private CPADAO initCPADAOMock() throws IOException, JAXBException
	{
		val result = Mockito.mock(CPADAO.class);
		Mockito.when(result.getCPA(cpaId)).thenReturn(loadCPA(cpaId));
		return result;
	}

	private Optional<CollaborationProtocolAgreement> loadCPA(String cpaId) throws IOException, JAXBException
	{
		val s = IOUtils.toString(this.getClass().getResourceAsStream("/nl/clockwork/ebms/cpa/" + cpaId + ".xml"),Charset.forName("UTF-8"));
		return Optional.of(JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(s));
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

	private EbMSSignatureGenerator initSignatureGenerator(CPAManager cpaManager) throws Exception
	{
		return new EbMSSignatureGenerator(cpaManager,EbMSKeyStore.of(keyStoreType,keyStorePath,keyStorePassword,keyStorePassword));
	}

	private EbMSSignatureValidator initSignatureValidator(CPAManager cpaManager) throws Exception
	{
		val trustStore = EbMSTrustStore.of(keyStoreType,keyStorePath,keyStorePassword);
		return new EbMSSignatureValidator(cpaManager,trustStore);
	}

	private EbMSMessage createMessage() throws EbMSProcessorException
	{
		val message = createMessage(cpaId);
		val result = messageFactory.createEbMSMessage(message);
		return result;
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
