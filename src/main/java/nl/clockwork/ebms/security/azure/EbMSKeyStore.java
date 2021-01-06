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
package nl.clockwork.ebms.security.azure;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.NonNull;

public class EbMSKeyStore extends nl.clockwork.ebms.security.EbMSKeyStore
{
	private static Map<String,EbMSKeyStore> keyStores = new ConcurrentHashMap<>();
	
	public static EbMSKeyStore of(@NonNull String uri, @NonNull String managedIdentity, String keyPassword) throws GeneralSecurityException, IOException
	{
		if (!keyStores.containsKey(uri))
			keyStores.put(uri,new EbMSKeyStore(uri,managedIdentity,keyPassword,null));
		return keyStores.get(uri);
	}

	public static EbMSKeyStore of(@NonNull String uri, @NonNull String managedIdentity, String keyPassword, String defaultAlias) throws GeneralSecurityException, IOException
	{
		String key = uri + defaultAlias;
		if (!keyStores.containsKey(key))
			keyStores.put(key,new EbMSKeyStore(uri,managedIdentity,keyPassword,defaultAlias));
		return keyStores.get(key);
	}

	private EbMSKeyStore(@NonNull String uri, @NonNull String managedIdentity, String keyPassword, String defaultAlias) throws GeneralSecurityException, IOException
	{
		super(uri,KeyStoreUtils.loadKeyStore(uri,managedIdentity),keyPassword,defaultAlias);
	}
}
