package nl.clockwork.ebms.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import nl.clockwork.ebms.common.util.SecurityUtils;

public class KeyStoreManager
{
	private static Map<String,KeyStore> keystores = new HashMap<String,KeyStore>();

	public static KeyStore getKeyStore(String path, String password) throws GeneralSecurityException, IOException
	{
		if (!keystores.containsKey(path))
			keystores.put(path,loadKeyStore(path,password));
		return keystores.get(path);
	}

	private static KeyStore loadKeyStore(String location, String password) throws GeneralSecurityException, IOException
	{
		//location = ResourceUtils.getURL(SystemPropertyUtils.resolvePlaceholders(location)).getFile();
		InputStream in = SecurityUtils.class.getResourceAsStream(location);
		if (in == null)
			in = SecurityUtils.class.getResourceAsStream("/" + location);
		if (in == null)
			in = new FileInputStream(location);
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(in,password.toCharArray());
		return keyStore;
	}

}
