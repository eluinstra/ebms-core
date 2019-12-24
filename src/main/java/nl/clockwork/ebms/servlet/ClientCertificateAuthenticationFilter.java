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

public class ClientCertificateAuthenticationFilter implements Filter
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private KeyStore trustStore;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try
		{
			String truststoreType = filterConfig.getInitParameter("truststoreType");
			String truststorePath = filterConfig.getInitParameter("truststorePath");
			String truststorePassword = filterConfig.getInitParameter("truststorePassword");
			trustStore = KeyStoreManager.getKeyStore(KeyStoreType.valueOf(truststoreType),truststorePath,truststorePassword);
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
			X509Certificate[] certificates = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
			if (validate(trustStore,certificates))
				chain.doFilter(request,response);
			else
				((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED,"Unauthorized");
		}
		catch (KeyStoreException e)
		{
			throw new ServletException(e);
		}
	}

	private boolean validate(KeyStore trustStore, X509Certificate[] certificates) throws KeyStoreException
	{
		return certificates != null && trustStore.getCertificateAlias(certificates[0]) != null;
	}

	@Override
	public void destroy()
	{
	}

}
