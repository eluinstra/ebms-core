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
package nl.clockwork.ebms.util;


import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.validation.ValidationException;
import org.apache.xml.security.encryption.XMLCipher;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtils
{
	private static final int KEYSIZE_192 = 192;
	private static final int KEYSIZE_128 = 128;
	private static final int KEYSIZE_256 = 256;

	public static KeyPair getKeyPair(EbMSKeyStore keyStore, String alias, String password) throws GeneralSecurityException
	{
		val key = keyStore.getKey(alias, password.toCharArray());
		if (key instanceof PrivateKey)
		{
			val cert = keyStore.getCertificate(alias);
			val publicKey = cert.getPublicKey();
			return new KeyPair(publicKey, (PrivateKey)key);
		}
		return null;
	}

	public static void validateCertificate(EbMSTrustStore trustStore, X509Certificate certificate, Instant date) throws KeyStoreException, ValidationException
	{
		try
		{
			certificate.checkValidity(Date.from(date));
			val aliases = trustStore.aliases();
			StreamUtils.toStream(aliases.asIterator())
					.map(findCertificate(trustStore))
					.flatMap(Optional::stream)
					.filter(matches(certificate))
					.filter(verifyWith(certificate))
					.findAny()
					.orElseThrow(() -> new ValidationException("Certificate " + certificate.getIssuerDN() + " not found!"));
		}
		catch (CertificateExpiredException | CertificateNotYetValidException e)
		{
			throw new ValidationException(e);
		}
	}

	private static Function<String, Optional<Certificate>> findCertificate(EbMSTrustStore trustStore)
	{
		return alias ->
		{
			try
			{
				return trustStore.getCertificate(alias);
			}
			catch (KeyStoreException e)
			{
				throw new EbMSProcessorException(e);
			}
		};
	}

	private static Predicate<? super Certificate> matches(X509Certificate certificate)
	{
		return c -> c instanceof X509Certificate && certificate.getIssuerDN().getName().equals(((X509Certificate)c).getSubjectDN().getName());
	}

	private static Predicate<? super Certificate> verifyWith(X509Certificate certificate)
	{
		return c ->
		{
			try
			{
				certificate.verify(c.getPublicKey());
				return true;
			}
			catch (GeneralSecurityException e)
			{
				log.warn("", e);
				return false;
			}
		};
	}

	public static SecretKey generateKey(String encryptionAlgorithm) throws NoSuchAlgorithmException
	{
		switch (encryptionAlgorithm)
		{
			case XMLCipher.AES_128:
				return generateKey("AES", KEYSIZE_128);
			case XMLCipher.AES_192:
				return generateKey("AES", KEYSIZE_192);
			case XMLCipher.AES_256:
				return generateKey("AES", KEYSIZE_256);
			case XMLCipher.TRIPLEDES:
				return generateKey("DESede", KEYSIZE_192);
			default:
				throw new NoSuchAlgorithmException(encryptionAlgorithm);
		}
	}

	private static SecretKey generateKey(String algorithm, int keysize) throws NoSuchAlgorithmException
	{
		val keyGenerator = KeyGenerator.getInstance(algorithm);
		keyGenerator.init(keysize);
		return keyGenerator.generateKey();
	}

}
