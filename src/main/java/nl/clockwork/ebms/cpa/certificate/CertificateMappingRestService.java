/*
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
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
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
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CertificateMappingRestService implements WithService
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
				return new nl.clockwork.ebms.cpa.certificate.CertificateMapping(parseCertificate(decodeBase64(source)),
						parseCertificate(decodeBase64(destination)),
						cpaId);
			}
			catch (CertificateException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	@NonNull
	CertificateMappingServiceImpl mappingService;

	@POST
	@Path("")
	public void setCertificateMapping(CertificateMapping certificateMapping)
	{
		try
		{
			mappingService.setCertificateMappingImpl(certificateMapping.toCertificateMapping());
		}
		catch (Exception e)
		{
			log.error("SetCertificateMapping " + certificateMapping,e);
			throw toWebApplicationException(e);
		}
	}

	@DELETE
	@Path("")
	@Consumes(MediaType.TEXT_PLAIN)
	public void deleteCertificateMapping(String source, @QueryParam("cpaId") String cpaId)
	{
		try
		{
			mappingService.deleteCertificateMappingImpl(parseCertificate(decodeBase64(source)),cpaId);
		}
		catch (Exception e)
		{
			log.error("DeleteCertificateMapping " + source,e);
			throw toWebApplicationException(e,MediaType.TEXT_PLAIN);
		}
	}

	@GET
	@Path("")
	public List<CertificateMapping> getCertificateMappings()
	{
		try
		{
			return mappingService.getCertificateMappingsImpl().stream().map(m -> CertificateMapping.of(m)).collect(Collectors.toList());
		}
		catch (Exception e)
		{
			log.error("GetCertificateMappings",e);
			throw toWebApplicationException(e);
		}
	}

	@DELETE
	@Path("cache")
	public void deleteCache()
	{
		try
		{
			mappingService.deleteCacheImpl();
		}
		catch (Exception e)
		{
			log.error("DeleteCache",e);
			throw toWebApplicationException(e);
		}
	}
}
