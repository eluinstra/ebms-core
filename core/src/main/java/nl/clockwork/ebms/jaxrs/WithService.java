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
package nl.clockwork.ebms.jaxrs;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import nl.clockwork.ebms.cpa.BadRequestException;
import nl.clockwork.ebms.cpa.CPANotFoundException;
import nl.clockwork.ebms.cpa.CPAServiceException;
import nl.clockwork.ebms.cpa.certificate.CertificateNotFoundException;
import nl.clockwork.ebms.cpa.url.URLNotFoundException;
import nl.clockwork.ebms.service.NotFoundException;

public interface WithService
{
	@Value
	public class Error
	{
		@NonNull
		String message;
	}

	default WebApplicationException toWebApplicationException(Exception exception) throws CPAServiceException
	{
		return toWebApplicationException(exception, MediaType.APPLICATION_JSON);
	}

	default WebApplicationException toWebApplicationException(Exception exception, String responseType)
	{
		val response = Match(exception).of(
				Case($(instanceOf(NotFoundException.class)), o -> Response.status(NOT_FOUND).type(responseType).build()),
				Case($(instanceOf(CPANotFoundException.class)), o -> Response.status(NOT_FOUND).type(responseType).build()),
				Case($(instanceOf(CertificateNotFoundException.class)), o -> Response.status(NOT_FOUND).type(responseType).build()),
				Case($(instanceOf(URLNotFoundException.class)), o -> Response.status(NOT_FOUND).type(responseType).build()),
				Case($(instanceOf(BadRequestException.class)), o -> Response.status(BAD_REQUEST).type(responseType).entity(exception.getMessage()).build()),
				Case($(), o -> Response.status(INTERNAL_SERVER_ERROR).type(responseType).entity(exception.getMessage()).build()));
		return new WebApplicationException(response);
	}
}
