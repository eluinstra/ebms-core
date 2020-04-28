package nl.clockwork.ebms.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
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
