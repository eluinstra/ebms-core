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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.validation.ClientCertificateManager;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCertificateManagerFilter implements Filter
{
	String x509CertificateHeader;
	boolean useX509Certificate;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		x509CertificateHeader = filterConfig.getInitParameter("x509CertificateHeader");
		useX509Certificate = StringUtils.isEmpty(x509CertificateHeader);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			if (useX509Certificate)
			{
				val certificates = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
				ClientCertificateManager.setCertificate(certificates != null && certificates.length > 0 ? certificates[0] : null);
			}
			else
			{
				val certificate = decode(((HttpServletRequest)request).getHeader(x509CertificateHeader));
				ClientCertificateManager.setCertificate(certificate);
			}
			log.info("User " + ClientCertificateManager.getCertificate().getSubjectDN().toString());
			chain.doFilter(request,response);
		}
		catch (CertificateException e)
		{
			throw new ServletException(e);
		}
	}

	private X509Certificate decode(String certificate) throws CertificateException
	{
		if (StringUtils.isBlank(certificate))
			return null;
		val is = new ByteArrayInputStream(certificate.getBytes(Charset.defaultCharset()));
		val cf = CertificateFactory.getInstance("X509");
		return (X509Certificate)cf.generateCertificate(is);
	}

	@Override
	public void destroy()
	{
	}
}
