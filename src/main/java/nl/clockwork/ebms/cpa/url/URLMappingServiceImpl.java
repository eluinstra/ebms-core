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


import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.jaxrs.WithService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Produces(MediaType.APPLICATION_JSON)
public class URLMappingServiceImpl implements URLMappingService, WithService
{
	@NonNull
	URLMapper urlMapper;

	@POST
	@Path("")
	@Override
	public void setURLMapping(URLMapping urlMapping) throws URLMappingServiceException
	{
		try
		{
			if (log.isDebugEnabled())
				log.debug("SetURLMapping " + urlMapping);
			urlMapper.setURLMapping(urlMapping);
		}
		catch (Exception e)
		{
			log.error("SetURLMapping " + urlMapping,e);
			throw toServiceException(new URLMappingServiceException(e));
		}
	}

	@DELETE
	@Path("{id}")
	@Override
	public void deleteURLMapping(@PathParam("id") String source) throws URLMappingServiceException
	{
		try
		{
			log.debug("DeleteURLMapping " + source);
			if (urlMapper.deleteURLMapping(source) == 0)
				throw new URLNotFoundException();
		}
		catch (URLMappingServiceException e)
		{
			log.error("DeleteURLMapping " + source,e);
			throw toServiceException(e);
		}
		catch (Exception e)
		{
			log.error("DeleteURLMapping " + source,e);
			throw toServiceException(new URLMappingServiceException(e));
		}
	}

	@GET
	@Path("")
	@Override
	public List<URLMapping> getURLMappings() throws URLMappingServiceException
	{
		try
		{
			log.debug("GetURLMappings");
			return urlMapper.getURLs();
		}
		catch (Exception e)
		{
			log.error("GetURLMappings",e);
			throw toServiceException(new URLMappingServiceException(e));
		}
	}

	@DELETE
	@Path("cache")
	@Override
	public void deleteCache()
	{
		urlMapper.deleteCache();
	}
}
