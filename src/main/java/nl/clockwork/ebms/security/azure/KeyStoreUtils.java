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


import com.azure.security.keyvault.jca.KeyVaultJcaProvider;
import com.azure.security.keyvault.jca.KeyVaultLoadStoreParameter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyStoreUtils
{
	public static KeyStore loadKeyStore(String keyvaultURI, String tennantID, String clientID, String clientSecret) throws GeneralSecurityException, IOException
	{
		if (Security.getProvider("AzureKeyVault") == null)
			Security.addProvider(new KeyVaultJcaProvider());
		val result = KeyStore.getInstance("AzureKeyVault");
		// aadUri is niet verplicht, deze is alleen nodig voor de afwijkende security zones de speciale germany zone + ..
		val parameter = new KeyVaultLoadStoreParameter(keyvaultURI, tennantID, clientID, clientSecret);
		result.load(parameter);
		return result;
	}
}
