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
package nl.clockwork.ebms.cpa;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.jaxb.JAXBParser;
import nl.clockwork.ebms.jaxrs.WithService;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.XSDValidator;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Path("cpas")
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
	public void validateCPA(/*CollaborationProtocolAgreement*/String cpa) throws CPAServiceException
	{
		try
		{
			log.debug("ValidateCPA");
			xsdValidator.validate(cpa);
			val cpa_ = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(cpa);
			log.info("Validating CPA " + cpa_.getCpaid());
			cpaValidator.validate(cpa_);
		}
		catch (Exception e)
		{
			log.error("ValidateCPA\n" + cpa,e);
			throwServiceException(new CPAServiceException(e));
		}
	}
	
	@POST
	@Path("{overwrite}")
	@Produces("text/plain")
	@Override
	public String insertCPA(/*CollaborationProtocolAgreement*/String cpa, @PathParam("overwrite") Boolean overwrite) throws CPAServiceException
	{
		try
		{
			log.debug("InsertCPA");
			xsdValidator.validate(cpa);
			val cpa_ = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(cpa);
			new CPAValidator(cpaManager).validate(cpa_);
			cpaManager.setCPA(cpa_,overwrite);
			log.debug("InsertCPA done");
			return cpa_.getCpaid();
		}
		catch (Exception e)
		{
			log.error("InsertCPA\n" + cpa,e);
			throwServiceException(new CPAServiceException(e));
			return null;
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
				throw new IllegalArgumentException("Could not delete CPA " + cpaId + "! CPA does not exists.");
		}
		catch (Exception e)
		{
			log.error("DeleteCPA " + cpaId,e);
			throwServiceException(new CPAServiceException(e));
		}
	}

	@GET
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
			log.error("GetCPAIds",e);
			throwServiceException(new CPAServiceException(e));
			return null;
		}
	}

	@GET
	@Path("{cpaId}")
	@Produces("text/xml")
	@Override
	public /*CollaborationProtocolAgreement*/String getCPA(@PathParam("cpaId") String cpaId) throws CPAServiceException
	{
		try
		{
			log.debug("GetCPAId " + cpaId);
			return JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpaManager.getCPA(cpaId).orElse(null));
		}
		catch (Exception e)
		{
			log.error("GetCPAId " + cpaId,e);
			throwServiceException(new CPAServiceException(e));
			return null;
		}
	}
}
