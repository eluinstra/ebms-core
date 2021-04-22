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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;

//import com.azure.identity.ClientSecretCredential;
//import com.azure.identity.ClientSecretCredentialBuilder;
//import com.azure.identity.ManagedIdentityCredential;
//import com.azure.identity.ManagedIdentityCredentialBuilder;
//import com.azure.security.keyvault.certificates.CertificateClient;
//import com.azure.security.keyvault.certificates.CertificateClientBuilder;
//import com.azure.security.keyvault.certificates.models.CertificateProperties;
//import com.azure.security.keyvault.certificates.models.KeyVaultCertificate;
//import com.azure.security.keyvault.certificates.models.KeyVaultCertificateWithPolicy;
//import com.azure.security.keyvault.keys.KeyClient;
//import com.azure.security.keyvault.keys.KeyClientBuilder;
//import com.azure.security.keyvault.keys.models.KeyVaultKey;
//import com.azure.security.keyvault.secrets.SecretClient;
//import com.azure.security.keyvault.secrets.SecretClientBuilder;
//import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import lombok.val;
import nl.clockwork.ebms.security.azure.KeyStoreUtils;

class KeyStoreUtilsTest {

//	@Test
	void testLoadKeyStore() throws GeneralSecurityException, IOException {
        val keyvaultURI = "https://ebmskvt.vault.azure.net/";
        val tennantID = "e3c5bc88-7ed8-4978-b0d4-1b3e90ed9f06";
        val clientID = "http://ebmsapp";
        val clientSecret = "_17c.3xulKW~2crwjVTFRT8n-5LKo44uF5";
        System.setProperty("azure.keyvault.uri", keyvaultURI);
        
		KeyStore ks = KeyStoreUtils.loadKeyStore(keyvaultURI, tennantID, clientID, clientSecret);
		assertNotNull(ks);
		Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) {
			String alieAs = aliases.nextElement();
			System.out.println(alieAs);
		}
		java.security.cert.Certificate cert = ks.getCertificate("ebmskeyvault1");
		assertNotNull(cert);
		System.out.println(cert.getType());
	}
	
//	@Test
//	void funnyBusiness() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
//	    ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
//	            .clientId("<USER ASSIGNED MANAGED IDENTITY CLIENT ID>") // only required for user assigned
//	            .build();
//	
//		ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
//	            .clientId("http://ebmsapp")
//	            .clientSecret("_17c.3xulKW~2crwjVTFRT8n-5LKo44uF5")
//	            .tenantId("e3c5bc88-7ed8-4978-b0d4-1b3e90ed9f06")
//	            .build();
//		CertificateClient certificateClient = new CertificateClientBuilder()
//			    .vaultUrl("https://ebmskvt.vault.azure.net/")
//			    .credential(clientSecretCredential)
//			    .buildClient();
//		SecretClient secretClient = new SecretClientBuilder()
//			    .vaultUrl("https://ebmskvt.vault.azure.net/")
//			    .credential(clientSecretCredential)
//			    .buildClient();
//		
//        for (CertificateProperties certificate : certificateClient.listPropertiesOfCertificates()) {
//            KeyVaultCertificate certificateWithAllProperties = certificateClient
//                .getCertificateVersion(certificate.getName(), certificate.getVersion());
//            System.out.printf("Received certificate with name %s and secret id %s\n", certificateWithAllProperties
//                    .getProperties().getName(),
//                certificateWithAllProperties.getSecretId());
//        }
//        
//		String alias = "kvgenerated";
//		String pass = "henk";
//		KeyStore ksout = KeyStore.getInstance("PKCS12");
//		ksout.load(null,null);
//        FileOutputStream fos = new FileOutputStream(new File("/Users/jwe20104/test.p12"));
//
//        KeyVaultCertificateWithPolicy kvcert = certificateClient.getCertificate(alias);
//		KeyVaultSecret secret = secretClient.getSecret(kvcert.getName(), kvcert.getProperties().getVersion());
//
//		if ( "application/x-pkcs12".equalsIgnoreCase(secret.getProperties().getContentType()) ) {
//			try {
////				X509Certificate nicecert = fromSecret(secret.getValue());
////				ksout.setCertificateEntry(alias, nicecert);
//				ImmutablePair<Key, Certificate[]> privkey = secretToKey(secret.getValue());
//				ksout.setKeyEntry(alias, privkey.getLeft(), pass.toCharArray(), privkey.getRight());
//			} catch (UnrecoverableKeyException e) {
//				e.printStackTrace();
//			} catch (NoSuchAlgorithmException e) {
//				e.printStackTrace();
//			} catch (CertificateException e) {
//				e.printStackTrace();
//			} catch (KeyStoreException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		ksout.store(fos, pass.toCharArray());
//		fos.flush();
//		fos.close();
//	}
	

	private X509Certificate fromSecret(String secret) throws CertificateException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(secret));
		return (X509Certificate) cf.generateCertificate(in);
	}
	
	private ImmutablePair<Key, Certificate[]> secretToKey(String secret) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, UnrecoverableKeyException {
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(new ByteArrayInputStream(Base64.getDecoder().decode(secret)),
		        "".toCharArray());
		String generatedAlias = ks.aliases().nextElement();
		return ImmutablePair.of(ks.getKey(generatedAlias, "".toCharArray())
				, ks.getCertificateChain(generatedAlias));
	}

}
