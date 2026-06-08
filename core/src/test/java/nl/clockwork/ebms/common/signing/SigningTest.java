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
package nl.clockwork.ebms.common.signing;

import static nl.clockwork.ebms.api.cpa.CPATestUtils.cpaCache;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.soap.SOAPException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
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
import nl.clockwork.ebms.common.message.EbMSMessageUtils;
import nl.clockwork.ebms.common.model.EbMSAttachment;
import nl.clockwork.ebms.common.model.EbMSDocument;
import nl.clockwork.ebms.common.model.EbMSMessage;
import nl.clockwork.ebms.common.security.EbMSKeyStore;
import nl.clockwork.ebms.common.security.EbMSTrustStore;
import nl.clockwork.ebms.common.security.KeyStoreType;
import nl.clockwork.ebms.common.validation.ValidationException;
import nl.clockwork.ebms.common.validation.ValidatorException;
import nl.clockwork.ebms.server.processor.EbMSProcessorException;
import org.apache.xml.security.Init;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

@TestInstance(value = Lifecycle.PER_CLASS)
@FieldDefaults(level = AccessLevel.PRIVATE)
class SigningTest
{
	CPAManager cpaManager;
	EbMSMessageFactory messageFactory;
	String cpaId = "cpaStubEBF.rm.https.signed";
	KeyStoreType keyStoreType = KeyStoreType.JKS;
	String keyStorePath = "nl/clockwork/ebms/keystore.jks";
	String keyStorePassword = "my-secret-password";
	EbMSSignatureGenerator signatureGenerator;
	EbMSSignatureValidator signatureValidator;

	@BeforeAll
	void init()
	{
		MockitoAnnotations.openMocks(this);
		Init.init();
		cpaManager = initCPAManager();
		messageFactory = initMessageFactory(cpaManager);
		signatureGenerator = initSignatureGenerator(cpaManager);
		signatureValidator = initSignatureValidator(cpaManager);
	}

	@Test
	void testSiging() throws EbMSProcessorException, ValidatorException, SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException,
			TransformerFactoryConfigurationError, TransformerException
	{
		val message = createMessage();
		val document = EbMSMessageUtils.getEbMSDocument(message);
		signatureGenerator.generate(document, message);
		signatureValidator.validate(document, message);
	}

	@Test
	void testSigingHeaderValidationFailure() throws EbMSProcessorException, ValidatorException, SOAPException, JAXBException, ParserConfigurationException,
			SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val message = createMessage();
		val document = EbMSMessageUtils.getEbMSDocument(message);
		signatureGenerator.generate(document, message);
		changeConversationId(document);
		assertThatThrownBy(() -> signatureValidator.validate(document, message)).isInstanceOf(ValidationException.class);
	}

	@Test
	void testSigingAttachmentValidationFailure() throws EbMSProcessorException, ValidatorException, SOAPException, JAXBException, ParserConfigurationException,
			SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val message = createMessage();
		val document = EbMSMessageUtils.getEbMSDocument(message);
		signatureGenerator.generate(document, message);
		message.getAttachments().clear();
		message.getAttachments().addAll(createAttachments(message.getMessageHeader().getMessageData().getMessageId()));
		assertThatThrownBy(() -> signatureValidator.validate(document, message)).isInstanceOf(ValidationException.class);
	}

	private void changeConversationId(EbMSDocument message)
	{
		val d = message.getMessage();
		val conversationId = d.getElementsByTagNameNS("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "ConversationId").item(0);
		conversationId.setTextContent(conversationId.getTextContent() + "0");
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

	private EbMSSignatureGenerator initSignatureGenerator(CPAManager cpaManager)
	{
		return new EbMSSignatureGenerator(cpaManager, EbMSKeyStore.of(keyStoreType, keyStorePath, keyStorePassword, keyStorePassword));
	}

	private EbMSSignatureValidator initSignatureValidator(CPAManager cpaManager)
	{
		val trustStore = EbMSTrustStore.of(keyStoreType, keyStorePath, keyStorePassword);
		return new EbMSSignatureValidator(cpaManager, trustStore);
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
