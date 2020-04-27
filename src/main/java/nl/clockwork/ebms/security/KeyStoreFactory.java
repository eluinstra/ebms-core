package nl.clockwork.ebms.security;

import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level=AccessLevel.PRIVATE, makeFinal=true)
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
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
