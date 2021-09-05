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
package nl.clockwork.ebms.jaxrs;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.phase.PhaseInterceptorChain;

import lombok.NonNull;
import lombok.Value;
import lombok.val;
import nl.clockwork.ebms.cpa.CPANotFoundException;
import nl.clockwork.ebms.cpa.CPAServiceException;
import nl.clockwork.ebms.cpa.certificate.CertificateMappingServiceException;
import nl.clockwork.ebms.cpa.certificate.CertificateNotFoundException;
import nl.clockwork.ebms.cpa.url.URLMappingServiceException;
import nl.clockwork.ebms.cpa.url.URLNotFoundException;
import nl.clockwork.ebms.service.EbMSMessageServiceException;
import nl.clockwork.ebms.service.NotFoundException;

public interface WithService
{
  @Value
  public class Error
  {
    @NonNull
    String message;
  }

  default CPAServiceException toServiceException(CPAServiceException exception) throws CPAServiceException
  {
    val message = PhaseInterceptorChain.getCurrentMessage();
    val servletRequest = (HttpServletRequest)message.get("HTTP.REQUEST");
    if (servletRequest.getContentType() == null || servletRequest.getContentType().equals(MediaType.APPLICATION_JSON))
  	{
      val response = Match(exception).of(
				Case($(instanceOf(CPANotFoundException.class)),o -> Response.status(NOT_FOUND)
						.type(MediaType.APPLICATION_JSON)
						.build()),
				Case($(),o ->	Response.status(INTERNAL_SERVER_ERROR)
						.type(MediaType.APPLICATION_JSON)
						.entity(exception.getMessage())
						.build()));
      throw new WebApplicationException(response);
    }
  	else
      return exception;
  }
	
  default CertificateMappingServiceException toServiceException(CertificateMappingServiceException exception) throws CertificateMappingServiceException
  {
    val message = PhaseInterceptorChain.getCurrentMessage();
    val servletRequest = (HttpServletRequest)message.get("HTTP.REQUEST");
    if (servletRequest.getContentType() == null || servletRequest.getContentType().equals(MediaType.APPLICATION_JSON))
  	{
      val response = Match(exception).of(
				Case($(instanceOf(CertificateNotFoundException.class)),o -> Response.status(NOT_FOUND)
						.type(MediaType.APPLICATION_JSON)
						.build()),
				Case($(),o ->	Response.status(INTERNAL_SERVER_ERROR)
						.type(MediaType.APPLICATION_JSON)
						.entity(exception.getMessage())
						.build()));
      throw new WebApplicationException(response);
    }
  	else
      return exception;
  }
	
  default URLMappingServiceException toServiceException(URLMappingServiceException exception) throws URLMappingServiceException
  {
    val message = PhaseInterceptorChain.getCurrentMessage();
    val servletRequest = (HttpServletRequest)message.get("HTTP.REQUEST");
    if (servletRequest.getContentType() == null || servletRequest.getContentType().equals(MediaType.APPLICATION_JSON))
  	{
      val response = Match(exception).of(
				Case($(instanceOf(URLNotFoundException.class)),o -> Response.status(NOT_FOUND)
						.type(MediaType.APPLICATION_JSON)
						.build()),
				Case($(),o ->	Response.status(INTERNAL_SERVER_ERROR)
						.type(MediaType.APPLICATION_JSON)
						.entity(exception.getMessage())
						.build()));
      throw new WebApplicationException(response);
    }
  	else
      return exception;
  }
	
  default EbMSMessageServiceException toServiceException(EbMSMessageServiceException exception) throws EbMSMessageServiceException
  {
    val message = PhaseInterceptorChain.getCurrentMessage();
    val servletRequest = (HttpServletRequest)message.get("HTTP.REQUEST");
    if (servletRequest.getContentType() == null || servletRequest.getContentType().equals(MediaType.APPLICATION_JSON))
  	{
      val response = Match(exception).of(
				Case($(instanceOf(NotFoundException.class)),o -> Response.status(NOT_FOUND)
						.type(MediaType.APPLICATION_JSON)
						.build()),
				Case($(),o ->	Response.status(INTERNAL_SERVER_ERROR)
						.type(MediaType.APPLICATION_JSON)
						.entity(exception.getMessage())
						.build()));
      throw new WebApplicationException(response);
    }
  	else
      return exception;
  }
	
}
