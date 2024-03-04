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
package nl.clockwork.ebms.validation;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageDecrypter;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationConfig
{
	@Autowired
	CPAManager cpaManager;
	@Autowired
	CPAValidator cpaValidator;
	@Value("${https.clientCertificateAuthentication}")
	boolean clientCertificateValidatorEnabled;

	@Bean
	public EbMSMessageValidator messageValidator(EbMSDAO ebMSDAO, EbMSSignatureValidator signatureValidator, EbMSMessageDecrypter messageDecrypter)
	{
		return EbMSMessageValidator.builder()
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.clientCertificateValidator(ClientCertificateValidator.of(cpaManager, clientCertificateValidatorEnabled))
				.cpaValidator(cpaValidator)
				.messageHeaderValidator(new MessageHeaderValidator(ebMSDAO, cpaManager))
				.manifestValidator(new ManifestValidator())
				.signatureValidator(new SignatureValidator(cpaManager, signatureValidator))
				.messageDecrypter(messageDecrypter)
				.build();
	}

	@Bean
	public MessagePropertiesValidator messagePropertiesValidator()
	{
		return new MessagePropertiesValidator(cpaManager);
	}
}
