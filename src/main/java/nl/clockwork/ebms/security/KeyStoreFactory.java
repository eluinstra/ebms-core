package nl.clockwork.ebms.security;

import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level=AccessLevel.PRIVATE, makeFinal=true)
@AllArgsConstructor
@RequiredArgsConstructor
public class KeyStoreFactory implements FactoryBean<EbMSKeyStore>
{
	@NonNull
	KeyStoreType type;
	@NonNull
	String path;
	@NonNull
	String password;
	@NonNull
	String keyPassword;
	@NonFinal
	String defaultAlias;

	@Override
	public EbMSKeyStore getObject() throws Exception
	{
		val keyStore = KeyStoreUtils.loadKeyStore(type,path,password);
		return new EbMSKeyStore(path,keyStore,keyPassword,defaultAlias);
	}

	@Override
	public Class<?> getObjectType()
	{
		return EbMSKeyStore.class;
	}

}
