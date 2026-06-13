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
package nl.clockwork.ebms.api.certificate.rest;

import static nl.clockwork.ebms.common.cpa.certificate.X509CertificateConverter.parseCertificate;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.security.cert.CertificateException;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.api.WithController;
import nl.clockwork.ebms.api.certificate.model.CertificateMapping;
import nl.clockwork.ebms.api.certificate.soap.CertificateMappingControllerImpl;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CertificateMappingRestController implements WithController
{
	@NonNull
	CertificateMappingControllerImpl mappingService;

	@POST
	@Path("")
	public void setCertificateMapping(CertificateMapping certificateMapping)
	{
		execute(() -> mappingService.setCertificateMappingImpl(certificateMapping.toCertificateMapping()));
	}

	@DELETE
	@Path("")
	@Consumes(MediaType.TEXT_PLAIN)
	public void deleteCertificateMapping(String source, @QueryParam("cpaId") String cpaId)
	{
		execute(() ->
		{
			try
			{
				mappingService.deleteCertificateMappingImpl(parseCertificate(decodeBase64(source)), cpaId);
			}
			catch (CertificateException e)
			{
				throw new RuntimeException(e);
			}
		}, MediaType.TEXT_PLAIN);
	}

	@GET
	@Path("")
	public List<CertificateMapping> getCertificateMappings()
	{
		return execute(() -> mappingService.getCertificateMappingsImpl().stream().map(CertificateMapping::of).toList());
	}

	@DELETE
	@Path("cache")
	public void deleteCache()
	{
		execute(mappingService::deleteCacheImpl);
	}
}
