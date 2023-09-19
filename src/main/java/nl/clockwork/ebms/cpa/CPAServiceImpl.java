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
package nl.clockwork.ebms.cpa;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.jaxb.JAXBParser;
import nl.clockwork.ebms.jaxrs.WithService;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.XSDValidator;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Produces(MediaType.APPLICATION_JSON)
public class CPAServiceImpl implements CPAService, WithService
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	CPAValidator cpaValidator;
	XSDValidator xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/cpp-cpa-2_0.xsd");

	@POST
	@Path("validate")
	@Override
	public void validateCPA(/* CollaborationProtocolAgreement */String cpa) throws CPAServiceException
	{
		try
		{
			log.debug("ValidateCPA");
			xsdValidator.validate(cpa);
			val parsedCpa = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(cpa);
			log.info("Validating CPA " + parsedCpa.getCpaid());
			cpaValidator.validate(parsedCpa);
		}
		catch (Exception e)
		{
			log.error("ValidateCPA\n" + cpa, e);
			throw toServiceException(new CPAServiceException(e));
		}
	}

	@POST
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String insertCPA(/* CollaborationProtocolAgreement */String cpa, @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite)
			throws CPAServiceException
	{
		try
		{
			log.debug("InsertCPA");
			xsdValidator.validate(cpa);
			val parsedCpa = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(cpa);
			new CPAValidator(cpaManager).validate(parsedCpa);
			cpaManager.setCPA(parsedCpa, overwrite);
			log.debug("InsertCPA done");
			return parsedCpa.getCpaid();
		}
		catch (Exception e)
		{
			log.error("InsertCPA\n" + cpa, e);
			throw toServiceException(new CPAServiceException(e));
		}
	}

	@DELETE
	@Path("{cpaId}")
	@Override
	public void deleteCPA(@PathParam("cpaId") String cpaId) throws CPAServiceException
	{
		try
		{
			log.debug("DeleteCPA " + cpaId);
			if (cpaManager.deleteCPA(cpaId) == 0)
				throw new CPANotFoundException();
		}
		catch (CPAServiceException e)
		{
			log.error("DeleteCPA " + cpaId, e);
			throw toServiceException(e);
		}
		catch (Exception e)
		{
			log.error("DeleteCPA " + cpaId, e);
			throw toServiceException(new CPAServiceException(e));
		}
	}

	@GET
	@Path("")
	@Override
	public List<String> getCPAIds() throws CPAServiceException
	{
		try
		{
			log.debug("GetCPAIds");
			return cpaManager.getCPAIds();
		}
		catch (Exception e)
		{
			log.error("GetCPAIds", e);
			throw toServiceException(new CPAServiceException(e));
		}
	}

	@GET
	@Path("{cpaId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public /* CollaborationProtocolAgreement */String getCPA(@PathParam("cpaId") String cpaId) throws CPAServiceException
	{
		try
		{
			log.debug("GetCPAId " + cpaId);
			return JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpaManager.getCPA(cpaId).orElse(null));
		}
		catch (Exception e)
		{
			log.error("GetCPAId " + cpaId, e);
			throw toServiceException(new CPAServiceException(e));
		}
	}

	@DELETE
	@Path("cache")
	@Override
	public void deleteCache()
	{
		cpaManager.clearCache();
	}
}
