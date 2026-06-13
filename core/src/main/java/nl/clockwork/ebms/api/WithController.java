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
package nl.clockwork.ebms.api;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.Value;
import nl.clockwork.ebms.api.certificate.exception.CertificateMappingControllerException;
import nl.clockwork.ebms.api.certificate.exception.CertificateNotFoundException;
import nl.clockwork.ebms.api.cpa.exception.BadRequestException;
import nl.clockwork.ebms.api.cpa.exception.CPAControllerException;
import nl.clockwork.ebms.api.cpa.exception.CPANotFoundException;
import nl.clockwork.ebms.api.ebms.exception.EbMSControllerException;
import nl.clockwork.ebms.api.ebms.exception.NotFoundException;
import nl.clockwork.ebms.api.url.exception.URLMappingControllerException;
import nl.clockwork.ebms.api.url.exception.URLNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface WithController
{
	Logger log = LoggerFactory.getLogger(WithController.class);

	@Value
	public class Error
	{
		@NonNull
		String message;
	}

	default WebApplicationException toWebApplicationException(Exception exception) throws CPAControllerException
	{
		return toWebApplicationException(exception, MediaType.APPLICATION_JSON);
	}

	default WebApplicationException toWebApplicationException(Exception exception, String responseType)
	{
		Response response;
		if (exception instanceof NotFoundException)
			response = Response.status(NOT_FOUND).type(responseType).build();
		else if (exception instanceof CPANotFoundException)
			response = Response.status(NOT_FOUND).type(responseType).build();
		else if (exception instanceof CertificateNotFoundException)
			response = Response.status(NOT_FOUND).type(responseType).build();
		else if (exception instanceof URLNotFoundException)
			response = Response.status(NOT_FOUND).type(responseType).build();
		else if (exception instanceof BadRequestException)
			response = Response.status(BAD_REQUEST).type(responseType).entity(exception.getMessage()).build();
		else if (exception instanceof EbMSControllerException)
			response = Response.status(INTERNAL_SERVER_ERROR).type(responseType).entity(exception.getMessage()).build();
		else if (exception instanceof CertificateMappingControllerException)
			response = Response.status(INTERNAL_SERVER_ERROR).type(responseType).entity(exception.getMessage()).build();
		else if (exception instanceof URLMappingControllerException)
			response = Response.status(INTERNAL_SERVER_ERROR).type(responseType).entity(exception.getMessage()).build();
		else if (exception instanceof CPAControllerException)
			response = Response.status(INTERNAL_SERVER_ERROR).type(responseType).entity(exception.getMessage()).build();
		else
			response = Response.status(INTERNAL_SERVER_ERROR).type(responseType).entity(exception.getMessage()).build();
		return new WebApplicationException(response);
	}

	default <T> T execute(Supplier<T> action, String responseType)
	{
		try
		{
			return action.get();
		}
		catch (Exception e)
		{
			log.error("Exception in REST endpoint", e);
			throw toWebApplicationException(e, responseType);
		}
	}

	default <T> T execute(Supplier<T> action)
	{
		return execute(action, MediaType.APPLICATION_JSON);
	}

	default void execute(Runnable action, String responseType)
	{
		try
		{
			action.run();
		}
		catch (Exception e)
		{
			log.error("Exception in REST endpoint", e);
			throw toWebApplicationException(e, responseType);
		}
	}

	default void execute(Runnable action)
	{
		execute(action, MediaType.APPLICATION_JSON);
	}
}
