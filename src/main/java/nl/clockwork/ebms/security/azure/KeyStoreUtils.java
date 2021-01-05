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

import com.azure.security.keyvault.jca.KeyVaultLoadStoreParameter;

import lombok.val;

class KeyStoreUtils
{
	public static KeyStore loadKeyStore(String uri) throws GeneralSecurityException, IOException
	{
		val keyStore = KeyStore.getInstance("AzureKeyVault");
		val parameter = new KeyVaultLoadStoreParameter(uri);
		keyStore.load(parameter);
		return keyStore;
	}

	public static KeyStore loadKeyStore(String uri, String managedIdentity) throws GeneralSecurityException, IOException
	{
		val keyStore = KeyStore.getInstance("AzureKeyVault");
		val parameter = new KeyVaultLoadStoreParameter(uri,managedIdentity);
		keyStore.load(parameter);
		return keyStore;
	}

	public static KeyStore loadKeyStore(String uri, String aadAuthenticationUrl, String tenantId, String clientId, String clientSecret) throws GeneralSecurityException, IOException
	{
		val keyStore = KeyStore.getInstance("AzureKeyVault");
		val parameter = new KeyVaultLoadStoreParameter(uri,aadAuthenticationUrl,tenantId,clientId,clientSecret);
		keyStore.load(parameter);
		return keyStore;
	}

}
