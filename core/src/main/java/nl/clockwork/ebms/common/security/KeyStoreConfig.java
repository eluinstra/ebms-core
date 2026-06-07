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
package nl.clockwork.ebms.common.security;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Conditional(DefaultKeyStoreConfig.class)
@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeyStoreConfig
{
	@Value("${truststore.type}")
	KeyStoreType trustStoretype;
	@Value("${truststore.path}")
	String trustStorepath;
	@Value("${truststore.password}")
	String trustStorepassword;
	@Value("${client.keystore.type}")
	KeyStoreType clientKeyStoreType;
	@Value("${client.keystore.path}")
	String clientKeyStorePath;
	@Value("${client.keystore.password}")
	String clientKeyStorePassword;
	@Value("${client.keystore.keyPassword}")
	String clientKeyStoreKeyPassword;
	@Value("${client.keystore.defaultAlias}")
	String clientKeyStoreDefaultAlias;
	@Value("${signature.keystore.type}")
	KeyStoreType signatureKeyStoreType;
	@Value("${signature.keystore.path}")
	String signatureKeyStorePath;
	@Value("${signature.keystore.password}")
	String signatureKeyStorePassword;
	@Value("${signature.keystore.keyPassword}")
	String signatureKeyStoreKeyPassword;
	@Value("${encryption.keystore.type}")
	KeyStoreType encryptionKeyStoreType;
	@Value("${encryption.keystore.path}")
	String encryptionKeyStorePath;
	@Value("${encryption.keystore.password}")
	String encryptionKeyStorePassword;
	@Value("${encryption.keystore.keyPassword}")
	String encryptionKeyStoreKeyPassword;
	@Value("${ebms.security.allowDefaultSecrets:false}")
	boolean allowDefaultSecrets;

	@Bean
	public EbMSTrustStore trustStore()
	{
		validateSecrets();
		return EbMSTrustStore.of(trustStoretype, trustStorepath, trustStorepassword);
	}

	@Bean("clientKeyStore")
	public EbMSKeyStore clientKeyStore()
	{
		validateSecrets();
		return EbMSKeyStore.of(clientKeyStoreType, clientKeyStorePath, clientKeyStorePassword, clientKeyStoreKeyPassword, clientKeyStoreDefaultAlias);
	}

	@Bean("signatureKeyStore")
	public EbMSKeyStore signatureKeyStore()
	{
		validateSecrets();
		return EbMSKeyStore.of(signatureKeyStoreType, signatureKeyStorePath, signatureKeyStorePassword, signatureKeyStoreKeyPassword);
	}

	@Bean("encryptionKeyStore")
	public EbMSKeyStore encryptionKeyStore()
	{
		validateSecrets();
		return EbMSKeyStore.of(encryptionKeyStoreType, encryptionKeyStorePath, encryptionKeyStorePassword, encryptionKeyStoreKeyPassword);
	}

	private void validateSecrets()
	{
		if (allowDefaultSecrets)
			return;

		validateSecret("truststore.password", trustStorepassword);
		validateSecret("client.keystore.password", clientKeyStorePassword);
		validateSecret("client.keystore.keyPassword", clientKeyStoreKeyPassword);
		validateSecret("signature.keystore.password", signatureKeyStorePassword);
		validateSecret("signature.keystore.keyPassword", signatureKeyStoreKeyPassword);
		validateSecret("encryption.keystore.password", encryptionKeyStorePassword);
		validateSecret("encryption.keystore.keyPassword", encryptionKeyStoreKeyPassword);
	}

	private void validateSecret(String propertyName, String value)
	{
		if (value == null || value.trim().isEmpty() || "password".equals(value) || value.startsWith("CHANGE_ME"))
			throw new IllegalStateException(
					"Unsafe value configured for "
							+ propertyName
							+ ". Provide a strong secret or set ebms.security.allowDefaultSecrets=true explicitly for local/test use.");
	}
}
