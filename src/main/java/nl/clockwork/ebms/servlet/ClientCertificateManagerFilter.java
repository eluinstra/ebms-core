package nl.clockwork.ebms.servlet;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.StringUtils;

import nl.clockwork.ebms.validation.ClientCertificateManager;

public class ClientCertificateManagerFilter implements Filter
{
	private String x509CertificateHeader;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		x509CertificateHeader = filterConfig.getInitParameter("x509CertificateHeader");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		if (StringUtils.isEmpty(x509CertificateHeader))
		{
			X509Certificate[] certificates = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
			ClientCertificateManager.setCertificate(certificates != null && certificates.length > 0 ? certificates[0] : null);
		}
		else
		{
			X509Certificate certificate = (X509Certificate)request.getAttribute(x509CertificateHeader);
			ClientCertificateManager.setCertificate(certificate);
		}
	}

	@Override
	public void destroy()
	{
	}

}
