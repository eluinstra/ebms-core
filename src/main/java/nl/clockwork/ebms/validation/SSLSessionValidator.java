package nl.clockwork.ebms.validation;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Date;

import javax.security.cert.X509Certificate;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.SSLSession;
import nl.clockwork.ebms.util.CPAUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.beans.factory.InitializingBean;

public class SSLSessionValidator implements InitializingBean
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private boolean enabled;
	private CPAManager cpaManager;
	private String keyStorePath;
	private String keyStorePassword;
	private String trustStorePath;
	private String trustStorePassword;
	private KeyStore keyStore;
	private KeyStore trustStore;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		keyStore = KeyStoreManager.getKeyStore(keyStorePath,keyStorePassword);
		trustStore = KeyStoreManager.getKeyStore(trustStorePath,trustStorePassword);
	}

	public void validate(EbMSMessage message) throws ValidatorException
	{
		if (enabled)
			try
			{
				SSLSession sslSession = SSLSessionManager.getSSLSession();
				if (sslSession != null)
				{
					if (sslSession.getLocalCertificates() != null && sslSession.getLocalCertificates().length > 0)
					{
						java.security.cert.X509Certificate certificate = (java.security.cert.X509Certificate)sslSession.getLocalCertificates()[0];
						SecurityUtils.validateCertificate(keyStore,certificate,message.getMessageHeader().getMessageData().getTimestamp() == null ? new Date() : message.getMessageHeader().getMessageData().getTimestamp());
						if (!certificate.equals(getLocalCertificate(message.getMessageHeader())))
							throw new ValidationException("Invalid SSL Server Certificate!");
					}
					if (sslSession.getPeerCertificateChain() != null && sslSession.getPeerCertificateChain().length > 0)
					{
						X509Certificate certificate = sslSession.getPeerCertificateChain()[0];
						SecurityUtils.validateCertificate(trustStore,certificate,message.getMessageHeader().getMessageData().getTimestamp() == null ? new Date() : message.getMessageHeader().getMessageData().getTimestamp());
						if (!certificate.equals(getPeerCertificate(message.getMessageHeader())))
							throw new ValidationException("Invalid SSL Client Certificate!");
					}
				}
			}
			catch (GeneralSecurityException e)
			{
				new ValidatorException(e);
			}
	}

	private java.security.cert.X509Certificate getLocalCertificate(MessageHeader messageHeader)
	{
		try
		{
			DeliveryChannel deliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getTo().getPartyId()),messageHeader.getTo().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction());
			if (deliveryChannel != null)
				return CPAUtils.getX509Certificate(CPAUtils.getSigningCertificate(deliveryChannel));
			return null;
		}
		catch (CertificateException e)
		{
			logger.warn("",e);
			return null;
		}
	}

	private java.security.cert.X509Certificate getPeerCertificate(MessageHeader messageHeader)
	{
		try
		{
			DeliveryChannel deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId()),messageHeader.getFrom().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction());
			if (deliveryChannel != null)
				return CPAUtils.getX509Certificate(CPAUtils.getSigningCertificate(deliveryChannel));
			return null;
		}
		catch (CertificateException e)
		{
			logger.warn("",e);
			return null;
		}
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setKeyStorePath(String keyStorePath)
	{
		this.keyStorePath = keyStorePath;
	}

	public void setKeyStorePassword(String keyStorePassword)
	{
		this.keyStorePassword = keyStorePassword;
	}

	public void setTrustStorePath(String trustStorePath)
	{
		this.trustStorePath = trustStorePath;
	}

	public void setTrustStorePassword(String trustStorePassword)
	{
		this.trustStorePassword = trustStorePassword;
	}

}
