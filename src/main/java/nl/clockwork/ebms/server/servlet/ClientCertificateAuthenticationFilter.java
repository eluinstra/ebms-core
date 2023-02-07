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
package nl.clockwork.ebms.server.servlet;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.security.KeyStoreType;
import nl.clockwork.ebms.validation.ClientCertificateManager;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCertificateAuthenticationFilter implements Filter
{
	EbMSTrustStore trustStore;

	@Override
	public void init(FilterConfig config) throws ServletException
	{
		try
		{
			val trustStoreType = config.getInitParameter("trustStoreType");
			val trustStorePath = config.getInitParameter("trustStorePath");
			val trustStorePassword = config.getInitParameter("trustStorePassword");
			trustStore = EbMSTrustStore.of(KeyStoreType.valueOf(trustStoreType), trustStorePath, trustStorePassword);
		}
		catch (GeneralSecurityException | IOException e)
		{
			throw new ServletException(e);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			val certificate = ClientCertificateManager.getCertificate();
			if (validate(trustStore, certificate))
				chain.doFilter(request, response);
			else
				((HttpServletResponse)response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
		catch (KeyStoreException e)
		{
			throw new ServletException(e);
		}
	}

	private boolean validate(EbMSTrustStore trustStore, X509Certificate x509Certificate) throws KeyStoreException
	{
		return x509Certificate != null && trustStore.getCertificateAlias(x509Certificate).isPresent();
	}

	@Override
	public void destroy()
	{
		// do nothing
	}
}
