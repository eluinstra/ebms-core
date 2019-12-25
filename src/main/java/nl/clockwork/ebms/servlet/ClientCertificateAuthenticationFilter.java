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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.KeyStoreManager.KeyStoreType;

public class ClientCertificateAuthenticationFilter implements Filter
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private String x509CertificateHeader;
	private boolean useX509CertificateHeader;
	private KeyStore trustStore;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try
		{
			x509CertificateHeader = filterConfig.getInitParameter("x509CertificateHeader");
			useX509CertificateHeader = StringUtils.isNotBlank(x509CertificateHeader);
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
			X509Certificate certificate = getCertificate(request);
			if (validate(trustStore,certificate))
				chain.doFilter(request,response);
			else
				((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED,"Unauthorized");
		}
		catch (KeyStoreException | CertificateException e)
		{
			throw new ServletException(e);
		}
	}

	private X509Certificate getCertificate(ServletRequest request) throws CertificateException
	{
		if (!useX509CertificateHeader)
		{
			X509Certificate[] certificates = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
			return (certificates != null && certificates.length > 0 ? certificates[0] : null);
		}
		else
			return decode(request.getAttribute(x509CertificateHeader));
	}

	private X509Certificate decode(Object certificate) throws CertificateException
	{
		if (certificate != null)
		{
			if (certificate instanceof String)
			{
				String s = (String)certificate;
				if (StringUtils.isNotBlank(s))
				{
					byte[] c = Base64.getDecoder().decode(s);
					ByteArrayInputStream is = new ByteArrayInputStream(c);
					CertificateFactory cf = CertificateFactory.getInstance("X509");
					return (X509Certificate)cf.generateCertificate(is);			
				}
			}
			else if (certificate instanceof X509Certificate)
			{
				return (X509Certificate)certificate;
			}
		}
		return null;
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
