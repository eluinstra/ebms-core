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
package nl.clockwork.ebms.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import nl.clockwork.ebms.common.util.SecurityUtils;

public class KeyStoreManager
{
	private static Map<String,KeyStore> keystores = new HashMap<>();

	public static KeyStore getKeyStore(String path, String password) throws GeneralSecurityException, IOException
	{
		if (!keystores.containsKey(path))
			keystores.put(path,loadKeyStore(path,password));
		return keystores.get(path);
	}

	private static KeyStore loadKeyStore(String location, String password) throws GeneralSecurityException, IOException
	{
		//location = ResourceUtils.getURL(SystemPropertyUtils.resolvePlaceholders(location)).getFile();
		try (InputStream in = getInputStream(location))
		{
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(in,password.toCharArray());
			return keyStore;
		}
	}

	private static InputStream getInputStream(String location) throws FileNotFoundException
	{
		InputStream result = SecurityUtils.class.getResourceAsStream(location);
		if (result == null)
			result = SecurityUtils.class.getResourceAsStream("/" + location);
		if (result == null)
			result = new FileInputStream(location);
		return result;
	}

}
