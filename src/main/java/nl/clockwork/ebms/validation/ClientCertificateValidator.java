package nl.clockwork.ebms.validation;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.CPAUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

public class ClientCertificateValidator
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private boolean enabled;
	private CPAManager cpaManager;

	public void validate(EbMSMessage message) throws ValidatorException
	{
		if (enabled)
		{
			X509Certificate[] certificates = ClientCertificateManager.getCertificates();
			if (certificates != null && certificates.length > 0)
			{
				java.security.cert.X509Certificate certificate = certificates[0];
				if (!certificate.equals(getClientCertificate(message.getMessageHeader())))
					throw new ValidationException("Invalid SSL Client Certificate!");
			}
		}
	}

	private java.security.cert.X509Certificate getClientCertificate(MessageHeader messageHeader)
	{
		try
		{
			DeliveryChannel deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId()),messageHeader.getFrom().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction());
			if (deliveryChannel != null)
				return CPAUtils.getX509Certificate(CPAUtils.getClientCertificate(deliveryChannel));
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

}
