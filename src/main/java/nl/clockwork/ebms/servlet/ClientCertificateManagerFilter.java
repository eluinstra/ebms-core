package nl.clockwork.ebms.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

import org.apache.commons.lang3.StringUtils;

import nl.clockwork.ebms.validation.ClientCertificateManager;

public class ClientCertificateManagerFilter implements Filter
{
	private String x509CertificateHeader;
	private boolean useX509CertificateHeader;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		x509CertificateHeader = filterConfig.getInitParameter("x509CertificateHeader");
		useX509CertificateHeader = StringUtils.isEmpty(x509CertificateHeader);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			if (useX509CertificateHeader)
			{
				X509Certificate[] certificates = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
				ClientCertificateManager.setCertificate(certificates != null && certificates.length > 0 ? certificates[0] : null);
			}
			else
			{
				X509Certificate certificate = decode(request.getAttribute(x509CertificateHeader));
				ClientCertificateManager.setCertificate(certificate);
			}
		}
		catch (CertificateException e)
		{
			throw new ServletException(e);
		}
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

	@Override
	public void destroy()
	{
	}

}
