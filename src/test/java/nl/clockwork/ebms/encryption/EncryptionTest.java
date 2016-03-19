package nl.clockwork.ebms.encryption;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.Role;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.validation.ValidatorException;

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.Init;
import org.junit.Test;
import org.mockito.Mockito;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

public class EncryptionTest
{
	private CPAManager cpaManager;
	private EbMSMessageFactory messageFactory;
	String cpaId = "cpaStubEBF.rm.https.signed";
	private String keyStorePath = "keystore.jks";
	private String keyStorePassword = "password";
	EbMSMessageEncrypter messageEncrypter;
	EbMSMessageDecrypter messageDecrypter;

	public EncryptionTest() throws Exception
	{
		Init.init();
		cpaManager = initCPAManager();
		messageFactory = initMessageFactory(cpaManager);
		messageEncrypter = initMessageEncrypter(cpaManager);
		messageDecrypter = initMessageDecrypter(cpaManager);
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
		//Mockito.when(result.removeAll());
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

	@Test
	public void testEncryption() throws EbMSProcessorException, ValidatorException
	{
		EbMSMessage message = createMessage();
		messageEncrypter.encrypt(message);
		messageDecrypter.decrypt(message);
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
		result.add(new EbMSDataSource("test.txt","plain/text; charset=utf-8","Dit is een test.".getBytes(Charset.forName("UTF-8"))));
		return result;
	}

}
