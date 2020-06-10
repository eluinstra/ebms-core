package nl.clockwork.ebms.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageDecrypter;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationConfig
{
	@Autowired
	CPAManager cpaManager;
	@Autowired
	EbMSDAO ebMSDAO;
	@Value("${https.clientCertificateAuthentication}")
	boolean clientCertificateValidatorEnabled;
	@Autowired
	EbMSSignatureValidator signatureValidator;
	@Autowired
	EbMSMessageDecrypter messageDecrypter;

	@Bean
	public CPAValidator cpaValidator()
	{
		return new CPAValidator(cpaManager);
	}

	@Bean
	public EbMSMessageValidator messageValidator()
	{
		return EbMSMessageValidator.builder()
				.setEbMSDAO(ebMSDAO)
				.setCpaManager(cpaManager)
				.setClientCertificateValidator(ClientCertificateValidator.of(cpaManager,clientCertificateValidatorEnabled))
				.setCpaValidator(cpaValidator())
				.setMessageHeaderValidator(new MessageHeaderValidator(ebMSDAO,cpaManager))
				.setManifestValidator(new ManifestValidator())
				.setSignatureValidator(new SignatureValidator(cpaManager,signatureValidator))
				.setMessageDecrypter(messageDecrypter)
				.build();
	}

	@Bean
	public EbMSMessageContextValidator ebMSMessageContextValidator()
	{
		return new EbMSMessageContextValidator(cpaManager);
	}
}
