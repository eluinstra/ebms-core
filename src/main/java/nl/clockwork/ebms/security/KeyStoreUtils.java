package nl.clockwork.ebms.security;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import nl.clockwork.ebms.common.util.SecurityUtils;

public class KeyStoreUtils
{
	public static KeyStore loadKeyStore(KeyStoreType type, String location, String password) throws GeneralSecurityException, IOException
	{
		//location = ResourceUtils.getURL(SystemPropertyUtils.resolvePlaceholders(location)).getFile();
		try (InputStream in = getInputStream(location))
		{
			KeyStore keyStore = KeyStore.getInstance(type.name());
			keyStore.load(in,password.toCharArray());
			return keyStore;
		}
	}

	public static InputStream getInputStream(String location) throws FileNotFoundException
	{
		try
		{
			return new FileInputStream(location);
		}
		catch (FileNotFoundException e)
		{
			InputStream result = SecurityUtils.class.getResourceAsStream(location);
			if (result == null)
				result = SecurityUtils.class.getResourceAsStream("/" + location);
			if (result == null)
				throw e;
			return result;
		}
	}

}
