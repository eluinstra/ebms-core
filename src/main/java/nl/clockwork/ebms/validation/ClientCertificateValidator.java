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
package nl.clockwork.ebms.validation;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import nl.clockwork.ebms.StreamUtils;
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
	protected transient final Log logger = LogFactory.getLog(getClass());
//	private static ClientCertificateValidator instance;
	private CPAManager cpaManager;

//	public static ClientCertificateValidator getInstance(boolean enabled, CPAManager cpaManager)
//	{
//		instance = instance != null ? instance :
//				enabled ? new ClientCertificateValidator(cpaManager) :
//					new ClientCertificateValidator()
//					{
//						@Override
//						public void validate(EbMSMessage message) throws ValidatorException
//						{
//						}
//					};
//		return instance;
//	}

	public static ClientCertificateValidator createInstance(boolean enabled, CPAManager cpaManager)
	{
		return enabled ? new ClientCertificateValidator(cpaManager) :
				new ClientCertificateValidator()
				{
					@Override
					public void validate(EbMSMessage message) throws ValidatorException
					{
					}
				};
	}

	private ClientCertificateValidator()
	{
	}

	private ClientCertificateValidator(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void validate(EbMSMessage message) throws ValidatorException
	{
		X509Certificate certificate = ClientCertificateManager.getCertificate();
		if (certificate != null)
		{
			if (!certificate.equals(getClientCertificate(message.getMessageHeader())))
				throw new ValidationException("Invalid SSL Client Certificate!");
		}
		else
			logger.warn("No certificates found.");
	}

	private X509Certificate getClientCertificate(MessageHeader messageHeader)
	{
		try
		{
			CacheablePartyId fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
			String service = CPAUtils.toString(messageHeader.getService());
			DeliveryChannel deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction())
					.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
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

}
