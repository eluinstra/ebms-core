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
package nl.clockwork.ebms.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import lombok.NonNull;
import lombok.Value;

@Value
public class EbMSTrustStore
{
	@NonNull
	KeyStore keyStore;

	public EbMSTrustStore(@NonNull KeyStoreType type, @NonNull String path, @NonNull String password) throws GeneralSecurityException, IOException
	{
		this.keyStore = KeyStoreUtils.loadKeyStore(type,path,password);
	}
	
	public Enumeration<String> aliases() throws KeyStoreException
	{
		return keyStore.aliases();
	}

	public Certificate getCertificate(String alias) throws KeyStoreException
	{
		return keyStore.getCertificate(alias);
	}

	public String getCertificateAlias(X509Certificate cert) throws KeyStoreException
	{
		return keyStore.getCertificateAlias(cert);
	}
}
