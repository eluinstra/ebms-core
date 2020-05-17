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
package nl.clockwork.ebms.server.servlet;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.support.WebApplicationContextUtils;

import lombok.AccessLevel;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.validation.ClientCertificateManager;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCertificateAuthenticationFilter implements Filter
{
	EbMSTrustStore trustStore;

	@Override
	public void init(FilterConfig config) throws ServletException
	{
		val wac = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
		var id = config.getInitParameter("trustStore");
		if (id == null)
			id = "trustStore";
		trustStore = wac.getBean(id,EbMSTrustStore.class);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			val certificate = ClientCertificateManager.getCertificate();
			if (validate(trustStore,certificate))
				chain.doFilter(request,response);
			else
				((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED,"Unauthorized");
		}
		catch (KeyStoreException e)
		{
			throw new ServletException(e);
		}
	}

	private boolean validate(EbMSTrustStore trustStore, X509Certificate x509Certificate) throws KeyStoreException
	{
		return x509Certificate != null && trustStore.getCertificateAlias(x509Certificate) != null;
	}

	@Override
	public void destroy()
	{
	}

}
