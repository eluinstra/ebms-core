package nl.clockwork.ebms.validation;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Date;

import javax.security.cert.X509Certificate;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.CPAUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.beans.factory.InitializingBean;

public class SSLCertificateValidator implements InitializingBean
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected CPAManager cpaManager;
	private String trustStorePath;
	private String trustStorePassword;
	private KeyStore trustStore;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		trustStore = SecurityUtils.loadKeyStore(trustStorePath,trustStorePassword);
	}

	public void validate(EbMSMessage message) throws ValidatorException
	{
		try
		{
			X509Certificate certificate = CertificateManager.getCertificate();
			if (certificate != null)
			{
				SecurityUtils.validateCertificate(trustStore,certificate,message.getMessageHeader().getMessageData().getTimestamp() == null ? new Date() : message.getMessageHeader().getMessageData().getTimestamp());
				compare(certificate,getCertificate(message.getMessageHeader()));
			}
		}
		catch (GeneralSecurityException e)
		{
			new ValidatorException(e);
		}
	}

	private void compare(X509Certificate certificate, java.security.cert.X509Certificate certificate2) throws ValidationException
	{
		if (!certificate.equals(certificate2))
			throw new ValidationException("Invalid Client Certificate!");
	}

	private java.security.cert.X509Certificate getCertificate(MessageHeader messageHeader)
	{
		try
		{
			DeliveryChannel deliveryChannel = cpaManager.getFromDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId()),messageHeader.getFrom().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction());
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

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
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
