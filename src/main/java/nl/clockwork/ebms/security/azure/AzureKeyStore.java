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
package nl.clockwork.ebms.security.azure;


import java.io.IOException;
import java.security.GeneralSecurityException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import nl.clockwork.ebms.security.EbMSKeyStore;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AzureKeyStore
{
	private static EbMSKeyStore keyStore = null;

	public static
			EbMSKeyStore
			of(@NonNull String keyvaultURI, @NonNull String tennantID, @NonNull String clientID, @NonNull String clientSecret, String defaultAlias)
					throws GeneralSecurityException, IOException
	{
		if (keyStore == null)
		{
			keyStore = new EbMSKeyStore("azure", KeyStoreUtils.loadKeyStore(keyvaultURI, tennantID, clientID, clientSecret), "", defaultAlias);
		}
		return keyStore;
	}

	public static EbMSKeyStore of(@NonNull String keyvaultURI, @NonNull String tennantID, @NonNull String clientID, @NonNull String clientSecret)
			throws GeneralSecurityException, IOException
	{
		if (keyStore == null)
		{
			keyStore = new EbMSKeyStore("azure", KeyStoreUtils.loadKeyStore(keyvaultURI, tennantID, clientID, clientSecret), "", "");
		}
		return keyStore;
	}
}
