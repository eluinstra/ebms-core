package nl.clockwork.ebms.jaxrs;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import nl.clockwork.ebms.jaxb.X509CertificateConverter;

public class X509CertificateSerializer extends StdSerializer<X509Certificate>
{

  public X509CertificateSerializer()
  {
    this(null);
  }

  protected X509CertificateSerializer(Class<X509Certificate> c)
  {
    super(c);
  }

  @Override
  public void serialize(X509Certificate value, JsonGenerator gen, SerializerProvider provider) throws IOException
  {
    try
    {
      gen.writeBinary(X509CertificateConverter.printCertificate(value));
    }
    catch (CertificateEncodingException e)
    {
      throw new IOException(e);
    }
  }
  
}
