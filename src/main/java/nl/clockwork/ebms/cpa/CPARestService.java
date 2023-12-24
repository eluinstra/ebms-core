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
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.jaxrs.WithService;
import org.xml.sax.SAXException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CPARestService implements WithService
{
	@NonNull
	CPAServiceImpl cpaService;

	@POST
	@Path("validate")
	@Consumes(MediaType.TEXT_PLAIN)
	public void validateCPA(String cpa)
	{
		try
		{
			cpaService.validateCPAImpl(cpa);
		}
		catch (SAXException | IllegalArgumentException e)
		{
			log.error("ValidateCPA\n" + cpa, e);
			throw toWebApplicationException(new BadRequestException(e));
		}
		catch (Exception e)
		{
			log.error("ValidateCPA\n" + cpa, e);
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
			return cpaService.insertCPAImpl(cpa, overwrite);
		}
		catch (SAXException | IllegalArgumentException e)
		{
			log.error("InsertCPA\n" + cpa, e);
			throw toWebApplicationException(new BadRequestException(e), MediaType.TEXT_PLAIN);
		}
		catch (Exception e)
		{
			log.error("InsertCPA\n" + cpa, e);
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}

	@DELETE
	@Path("{cpaId}")
	public void deleteCPA(@PathParam("cpaId") String cpaId)
	{
		try
		{
			cpaService.deleteCPAImpl(cpaId);
		}
		catch (Exception e)
		{
			log.error("DeleteCPA " + cpaId, e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("")
	public List<String> getCPAIds()
	{
		try
		{
			return cpaService.getCPAIdsImpl();
		}
		catch (Exception e)
		{
			log.error("GetCPAIds", e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("{cpaId}")
	@Produces({MediaType.TEXT_PLAIN})
	public String getCPA(@PathParam("cpaId") String cpaId)
	{
		try
		{
			return cpaService.getCPAImpl(cpaId);
		}
		catch (Exception e)
		{
			log.error("GetCPAId " + cpaId, e);
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}

	@DELETE
	@Path("cache")
	public void deleteCache()
	{
		try
		{
			cpaService.deleteCacheImpl();
		}
		catch (Exception e)
		{
			log.error("DeleteCache", e);
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}
}
