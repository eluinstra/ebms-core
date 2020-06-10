package nl.clockwork.ebms.encryption;

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
public class EncryptionConfig
{
	@Autowired
	CPAManager cpaManager;
	@Autowired
	EbMSTrustStore trustStore;
	@Autowired
	@Qualifier("encryptionKeyStore")
	EbMSKeyStore keyStore;

	@Bean
	public EbMSMessageEncrypter messageEncrypter()
	{
		return new EbMSMessageEncrypter(cpaManager,trustStore);
	}

	@Bean
	public EbMSMessageDecrypter messageDecrypter()
	{
		return new EbMSMessageDecrypter(cpaManager,keyStore);
	}
}
