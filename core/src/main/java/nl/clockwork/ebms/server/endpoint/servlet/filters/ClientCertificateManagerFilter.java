/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms.server.endpoint.servlet.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.common.security.EbMSTrustStore;
import nl.clockwork.ebms.common.security.KeyStoreType;
import nl.clockwork.ebms.server.security.certificate.ClientCertificateManager;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCertificateManagerFilter implements Filter
{
	String x509CertificateHeader;
	boolean useX509CertificateHeader;
	// F2: truststore used to validate a client certificate presented over the HTTP header
	// (reverse-proxy mode). When a header is configured but no truststore is, header-derived
	// certificates are rejected (fail closed) rather than trusted blindly.
	EbMSTrustStore trustStore;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		x509CertificateHeader = filterConfig.getInitParameter("x509CertificateHeader");
		useX509CertificateHeader = StringUtils.isEmpty(x509CertificateHeader);
		if (!useX509CertificateHeader)
		{
			val type = filterConfig.getInitParameter("trustStoreType");
			val path = filterConfig.getInitParameter("trustStorePath");
			val password = filterConfig.getInitParameter("trustStorePassword");
			trustStore = (StringUtils.isEmpty(path) || StringUtils.isEmpty(password)) ? null : EbMSTrustStore.of(KeyStoreType.valueOf(type), path, password);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			val certificate = useX509CertificateHeader ? certificateFromRequest(request) : certificateFromHeader(request, (HttpServletResponse)response);
			ClientCertificateManager.setCertificate(certificate);
			log.info("Certificate " + (certificate != null ? certificate.getSubjectX500Principal().toString() : " not found!"));
			chain.doFilter(request, response);
		}
		catch (CertificateException | KeyStoreException e)
		{
			throw new ServletException(e);
		}
		finally
		{
			ClientCertificateManager.clear();
		}
	}

	private X509Certificate certificateFromRequest(ServletRequest request)
	{
		val certificates = (X509Certificate[])request.getAttribute("jakarta.servlet.request.X509Certificate");
		return certificates != null && certificates.length > 0 ? certificates[0] : null;
	}

	// F2: the certificate is decoded from a client-controlled header, so it must be checked
	// against the configured truststore before being accepted as the peer's identity. Without
	// this check an attacker could send any valid-looking certificate in the header and be
	// treated as that partner (identity / per-user rate-limit bypass).
	private X509Certificate certificateFromHeader(ServletRequest request, HttpServletResponse response) throws CertificateException, KeyStoreException
	{
		val header = ((HttpServletRequest)request).getHeader(x509CertificateHeader);
		val certificate = decode(URLDecoder.decode(header, java.nio.charset.StandardCharsets.UTF_8));
		if (certificate == null)
			return null;
		if (trustStore == null || trustStore.getCertificateAlias(certificate).isEmpty())
		{
			log.warn("Client certificate from header not present in trust store, rejecting.");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			throw new CertificateException("Client certificate not trusted");
		}
		return certificate;
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
		// do nothing
	}
}
