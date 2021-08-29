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
package nl.clockwork.ebms.cpa.certificate;

import static nl.clockwork.ebms.jaxb.X509CertificateConverter.parseCertificate;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.jaxrs.WithService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Produces(MediaType.APPLICATION_JSON)
public class CertificateMappingServiceImpl implements CertificateMappingService, WithService
{
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class CertificateMapping
	{
		@NonNull
		String source;
		@NonNull
		String destination;
		String cpaId;

		static CertificateMapping of(nl.clockwork.ebms.cpa.certificate.CertificateMapping m)
		{
			try
			{
				return new CertificateMapping(encodeBase64String(m.getSource().getEncoded()),encodeBase64String(m.getDestination().getEncoded()),m.getCpaId());
			}
			catch (CertificateEncodingException e)
			{
				throw new IllegalStateException(e);
			}
		}

		nl.clockwork.ebms.cpa.certificate.CertificateMapping toCertificateMapping()
		{
			try
			{
				return new nl.clockwork.ebms.cpa.certificate.CertificateMapping(parseCertificate(decodeBase64(source)),parseCertificate(decodeBase64(destination)),cpaId);
			}
			catch (CertificateException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

  @NonNull
	CertificateMapper certificateMapper;

	@POST
	@Path("")
	public void setCertificateMapping(CertificateMapping certificateMapping) throws CertificateMappingServiceException
	{
		try
		{
			setCertificateMapping(certificateMapping.toCertificateMapping());
		}
		catch (Exception e)
		{
			log.error("SetCertificateMapping " + certificateMapping,e);
			throw toServiceException(new CertificateMappingServiceException(e));
		}
	}

	@Override
	public void setCertificateMapping(nl.clockwork.ebms.cpa.certificate.CertificateMapping certificateMapping) throws CertificateMappingServiceException
	{
		try
		{
			if (log.isDebugEnabled())
				log.debug("SetCertificateMapping " + certificateMapping);
			certificateMapper.setCertificateMapping(certificateMapping);
		}
		catch (Exception e)
		{
			log.error("SetCertificateMapping " + certificateMapping,e);
			throw toServiceException(new CertificateMappingServiceException(e));
		}
	}

	@DELETE
	@Path("")
	public void deleteCertificateMapping(String source, @QueryParam("cpaId") String cpaId) throws CertificateMappingServiceException
	{
		try
		{
			deleteCertificateMapping(parseCertificate(decodeBase64(source)),cpaId);
		}
		catch (Exception e)
		{
			log.error("DeleteCertificateMapping " + source,e);
			throw toServiceException(new CertificateMappingServiceException(e));
		}
	}

	@Override
	public void deleteCertificateMapping(X509Certificate source, @QueryParam("cpaId") String cpaId) throws CertificateMappingServiceException
	{
		try
		{
			if (log.isDebugEnabled())
				log.debug("DeleteCertificateMapping " + source);
			if (certificateMapper.deleteCertificateMapping(source,cpaId) == 0)
				throw new CertificateNotFoundException();
		}
		catch (CertificateMappingServiceException e)
		{
			log.error("GetCertificateMappings",e);
			throw toServiceException(e);
		}
		catch (Exception e)
		{
			log.error("DeleteCertificateMapping " + source,e);
			throw toServiceException(new CertificateMappingServiceException(e));
		}
	}

	@GET
	@Path("")
	public List<CertificateMapping> getCertificateMappingsRest() throws CertificateMappingServiceException
	{
		try
		{
			return getCertificateMappings().stream().map(m -> CertificateMapping.of(m)).collect(Collectors.toList());
		}
		catch (Exception e)
		{
			log.error("GetCertificateMappings",e);
			throw toServiceException(new CertificateMappingServiceException(e));
		}
	}

	@Override
	public List<nl.clockwork.ebms.cpa.certificate.CertificateMapping> getCertificateMappings() throws CertificateMappingServiceException
	{
		try
		{
			log.debug("GetCertificateMappings");
			return certificateMapper.getCertificates();
		}
		catch (Exception e)
		{
			log.error("GetCertificateMappings",e);
			throw toServiceException(new CertificateMappingServiceException(e));
		}
	}
}
