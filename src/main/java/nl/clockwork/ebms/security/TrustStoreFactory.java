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
public class TrustStoreFactory implements FactoryBean<EbMSTrustStore>
{
	@NonNull
	KeyStoreType type;
	@NonNull
	String path;
	@NonNull
	String password;

	@Override
	public EbMSTrustStore getObject() throws Exception
	{
		val trustStore = KeyStoreUtils.loadKeyStore(type,path,password);
		return new EbMSTrustStore(trustStore);
	}

	@Override
	public Class<?> getObjectType()
	{
		return EbMSTrustStore.class;
	}

}
