package nl.clockwork.ebms.signing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SigningConfig
{
	@Autowired
	CPAManager cpaManager;
	@Autowired
	@Qualifier("signatureKeyStore")
	EbMSKeyStore keyStore;
	@Autowired
	EbMSTrustStore trustStore;

	@Bean
	public EbMSSignatureGenerator signatureGenerator()
	{
		return new EbMSSignatureGenerator(cpaManager,keyStore);
	}

	@Bean
	public EbMSSignatureValidator signatureValidator()
	{
		return new EbMSSignatureValidator(cpaManager,trustStore);
	}
}
