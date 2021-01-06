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
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.NonNull;

public class EbMSTrustStore extends nl.clockwork.ebms.security.EbMSTrustStore
{
	private static Map<String,EbMSTrustStore> trustStores = new ConcurrentHashMap<>();

	public static EbMSTrustStore of(@NonNull String uri, @NonNull String managedIdentity) throws GeneralSecurityException, IOException
	{
		if (!trustStores.containsKey(uri))
			trustStores.put(uri,new EbMSTrustStore(KeyStoreUtils.loadKeyStore(uri,managedIdentity)));
		return trustStores.get(uri);
	}
	
	private EbMSTrustStore(@NonNull KeyStore keyStore) throws GeneralSecurityException, IOException
	{
		super(keyStore);
	}
}
