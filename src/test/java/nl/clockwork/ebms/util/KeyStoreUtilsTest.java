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
package nl.clockwork.ebms.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Enumeration;

import org.junit.jupiter.api.Test;

import nl.clockwork.ebms.security.azure.KeyStoreUtils;

class KeyStoreUtilsTest {

//	@Test
	void testLoadKeyStore() throws GeneralSecurityException, IOException {
		KeyStore ks = KeyStoreUtils.loadKeyStore(null, null);
		assertNotNull(ks);
		Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) {
			String alieAs = aliases.nextElement();
			System.out.println(alieAs);
		}
		java.security.cert.Certificate cert = ks.getCertificate("digipoort-overheid");
		assertNotNull(cert);
		System.out.println(cert.getType());
	}

}
