package nl.clockwork.ebms.security;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level=AccessLevel.PRIVATE, makeFinal=true)
@Getter
@EqualsAndHashCode
@ToString
public class EbMSTrustStore
{
	@NonNull
	KeyStore keyStore;

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
