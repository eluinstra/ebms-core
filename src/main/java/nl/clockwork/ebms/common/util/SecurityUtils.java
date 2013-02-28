/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.common.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

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

}
