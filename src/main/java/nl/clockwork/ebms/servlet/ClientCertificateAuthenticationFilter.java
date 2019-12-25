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
package nl.clockwork.ebms.servlet;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.KeyStoreManager.KeyStoreType;
import nl.clockwork.ebms.validation.ClientCertificateManager;

public class ClientCertificateAuthenticationFilter implements Filter
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private KeyStore trustStore;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try
		{
			String trustStoreType = filterConfig.getInitParameter("trustStoreType");
			String trustStorePath = filterConfig.getInitParameter("trustStorePath");
			String trustStorePassword = filterConfig.getInitParameter("trustStorePassword");
			trustStore = KeyStoreManager.getKeyStore(KeyStoreType.valueOf(trustStoreType),trustStorePath,trustStorePassword);
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
			X509Certificate certificate = ClientCertificateManager.getCertificate();
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

	private boolean validate(KeyStore trustStore, X509Certificate x509Certificate) throws KeyStoreException
	{
		return x509Certificate != null && trustStore.getCertificateAlias(x509Certificate) != null;
	}

	@Override
	public void destroy()
	{
	}

}
