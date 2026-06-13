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
package nl.clockwork.ebms.api.cpa.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.api.WithController;
import nl.clockwork.ebms.api.cpa.exception.BadRequestException;
import nl.clockwork.ebms.api.cpa.soap.CPAControllerImpl;
import org.xml.sax.SAXException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CPARestController implements WithController
{
	@NonNull
	CPAControllerImpl cpaController;

	@POST
	@Path("validate")
	@Consumes(MediaType.TEXT_PLAIN)
	public void validateCPA(String cpa)
	{
		try
		{
			cpaController.validateCPAImpl(cpa);
		}
		catch (SAXException | IllegalArgumentException e)
		{
			throw toWebApplicationException(new BadRequestException(e));
		}
		catch (IOException | JAXBException | ParserConfigurationException e)
		{
			throw toWebApplicationException(e);
		}
		catch (RuntimeException e)
		{
			throw toWebApplicationException(e);
		}
	}

	@POST
	@Path("")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces({MediaType.TEXT_PLAIN})
	public String insertCPA(String cpa, @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite)
	{
		try
		{
			return cpaController.insertCPAImpl(cpa, overwrite);
		}
		catch (SAXException | IllegalArgumentException e)
		{
			throw toWebApplicationException(new BadRequestException(e), MediaType.TEXT_PLAIN);
		}
		catch (IOException | JAXBException | ParserConfigurationException e)
		{
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
		catch (RuntimeException e)
		{
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}

	@DELETE
	@Path("{cpaId}")
	public void deleteCPA(@PathParam("cpaId") String cpaId)
	{
		cpaController.deleteCPAImpl(cpaId);
	}

	@GET
	@Path("")
	public List<String> getCPAIds()
	{
		return cpaController.getCPAIdsImpl();
	}

	@GET
	@Path("{cpaId}")
	@Produces({MediaType.TEXT_PLAIN})
	public String getCPA(@PathParam("cpaId") String cpaId)
	{
		try
		{
			return cpaController.getCPAImpl(cpaId);
		}
		catch (JAXBException e)
		{
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}

	@DELETE
	@Path("cache")
	public void deleteCache()
	{
		cpaController.deleteCacheImpl();
	}
}
