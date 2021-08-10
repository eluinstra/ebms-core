package nl.clockwork.ebms.jaxrs;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.cxf.phase.PhaseInterceptorChain;

import lombok.Value;
import lombok.val;

public interface WithService
{
  @Value
  public class Error
  {
    String message;
  }

  default <T extends Exception> void throwServiceException(T exception) throws T
  {
    val message = PhaseInterceptorChain.getCurrentMessage();
    val servletRequest = (HttpServletRequest)message.get("HTTP.REQUEST");
    if (servletRequest.getContentType().equals("application/json"))
  	{
      val response = Response.status(INTERNAL_SERVER_ERROR)
  				.type("application/json")
  				.entity(new Error(exception.getMessage()))
  				.build();
      throw new WebApplicationException(response);
    }
  	else
      throw exception;
  }
	
}
