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
package nl.clockwork.ebms.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@Getter
@ToString
public class EbMSTrustStore
{
	private static Map<String, EbMSTrustStore> trustStores = new ConcurrentHashMap<>();
	@NonNull
	KeyStore keyStore;

	public static EbMSTrustStore of(KeyStoreType type, String path, String password) throws GeneralSecurityException, IOException
	{
		if (!trustStores.containsKey(path))
			trustStores.put(path, new EbMSTrustStore(KeyStoreUtils.loadKeyStore(type, path, password)));
		return trustStores.get(path);
	}

	public EbMSTrustStore(@NonNull KeyStore keyStore)
	{
		this.keyStore = keyStore;
	}

	public Enumeration<String> aliases() throws KeyStoreException
	{
		return keyStore.aliases();
	}

	public Optional<Certificate> getCertificate(String alias) throws KeyStoreException
	{
		return Optional.ofNullable(keyStore.getCertificate(alias));
	}

	public Optional<String> getCertificateAlias(X509Certificate cert) throws KeyStoreException
	{
		return Optional.ofNullable(keyStore.getCertificateAlias(cert));
	}
}
