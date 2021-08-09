package nl.clockwork.ebms.jaxrs;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import nl.clockwork.ebms.jaxb.X509CertificateConverter;

public class X509CertificateDeserializer extends StdDeserializer<X509Certificate>
{

	public X509CertificateDeserializer()
	{
		this(null);
	}

	X509CertificateDeserializer(Class<X509Certificate> c)
	{
		super(c);
	}

	@Override
	public X509Certificate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
	{
		try
		{
			return X509CertificateConverter.parseCertificate(p.getBinaryValue());
		}
		catch (CertificateException e)
		{
			throw new IOException(e);
		}
	}

}
