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
package nl.clockwork.ebms.cpa.url;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.jaxrs.WithService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class URLMappingRestService implements WithService
{
	@NonNull
	URLMappingServiceImpl mappingService;

	@POST
	@Path("")
	public void setURLMapping(URLMapping urlMapping)
	{
		try
		{
			mappingService.setURLMappingImpl(urlMapping);
		}
		catch (Exception e)
		{
			log.error("SetURLMapping " + urlMapping, e);
			throw toWebApplicationException(e);
		}
	}

	@DELETE
	@Path("{id}")
	public void deleteURLMapping(@PathParam("id") String source)
	{
		try
		{
			mappingService.deleteURLMappingImpl(source);
		}
		catch (Exception e)
		{
			log.error("DeleteURLMapping " + source, e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("")
	public List<URLMapping> getURLMappings()
	{
		try
		{
			return mappingService.getURLMappingsImpl();
		}
		catch (Exception e)
		{
			log.error("GetURLMappings", e);
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
			log.error("DeleteCache", e);
			throw toWebApplicationException(e);
		}
	}
}
