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
package nl.clockwork.ebms.cpa.url;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.phase.PhaseInterceptorChain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Path("/urlMapping")
public class URLMappingServiceImpl implements URLMappingService
{
  @NonNull
	URLMapper urlMapper;

	@POST
  @Produces(MediaType.APPLICATION_JSON)
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
			throwServiceException(new URLMappingServiceException(e));
		}
	}

  private static <T extends Exception> void throwServiceException(T exception) throws T
	{
    val message = PhaseInterceptorChain.getCurrentMessage();
    val servletRequest = (HttpServletRequest)message.get("HTTP.REQUEST");
    if (servletRequest.getContentType().equals("application/json"))
		{
      val response = Response.status(INTERNAL_SERVER_ERROR)
					.type("application/json")
					.entity(exception.getMessage())
					.build();
      throw new WebApplicationException(response);
    }
		else
      throw exception;
  }

	@DELETE
	@Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
	@Override
	public void deleteURLMapping(@PathParam("id") String source) throws URLMappingServiceException
	{
		try
		{
			log.debug("DeleteURLMapping " + source);
			urlMapper.deleteURLMapping(source);
		}
		catch (Exception e)
		{
			log.error("DeleteURLMapping " + source,e);
			throwServiceException(new URLMappingServiceException(e));
		}
	}

	@GET
  @Produces(MediaType.APPLICATION_JSON)
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
			throwServiceException(new URLMappingServiceException(e));
			return null;
		}
	}
}
