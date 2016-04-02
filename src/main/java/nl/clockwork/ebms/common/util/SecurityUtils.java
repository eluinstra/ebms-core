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
package nl.clockwork.ebms.common.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.xml.security.encryption.XMLCipher;

public class SecurityUtils
{
	public static KeyStore loadKeyStore(String location, String password) throws GeneralSecurityException, IOException
	{
		//location = ResourceUtils.getURL(SystemPropertyUtils.resolvePlaceholders(location)).getFile();
		InputStream in = SecurityUtils.class.getResourceAsStream(location);
		if (in == null)
			in = SecurityUtils.class.getResourceAsStream("/" + location);
		if (in == null)
			in = new FileInputStream(location);
		if (in == null)
			throw new IOException("Cannot find keystore " + location);
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(in,password.toCharArray());
		return keyStore;
	}

	public static KeyPair getKeyPair(KeyStore keyStore, String alias, String password) throws GeneralSecurityException
	{
		Key key = keyStore.getKey(alias,password.toCharArray());
		if (key instanceof PrivateKey)
		{
			Certificate cert = keyStore.getCertificate(alias);
			PublicKey publicKey = cert.getPublicKey();
			return new KeyPair(publicKey,(PrivateKey)key);
		}
		return null;
	}

	public static KeyPair getKeyPairByCertificateSubject(KeyStore keyStore, String subject, String password) throws GeneralSecurityException
	{
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements())
		{
			String alias = aliases.nextElement();
			X509Certificate certificate = (X509Certificate)keyStore.getCertificate(alias);
			if (certificate.getSubjectDN().getName().equals(subject))
			{
				Key key = keyStore.getKey(alias,password.toCharArray());
				if (key instanceof PrivateKey)
					return new KeyPair(certificate.getPublicKey(),(PrivateKey)key);
				break;
			}
		}
		return null;
	}

	public static SecretKey generateKey(String encryptionAlgorithm) throws NoSuchAlgorithmException
	{
		switch (encryptionAlgorithm)
		{
			case XMLCipher.AES_128:
				return generateKey("AES",128);
			case XMLCipher.AES_192:
				return generateKey("AES",192);
			case XMLCipher.AES_256:
				return generateKey("AES",256);
			case XMLCipher.TRIPLEDES:
				return generateKey("DESede",192);
			default:
				return null;
		}
	}

	private static SecretKey generateKey(String algorithm, int keysize) throws NoSuchAlgorithmException
	{
		KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
		keyGenerator.init(keysize);
		return keyGenerator.generateKey();
	}

}
