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

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSBaseMessage;

@CommonsLog
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientCertificateValidator
{
	private static class DisabledClientCertificateValidator extends ClientCertificateValidator
	{
		public DisabledClientCertificateValidator(CPAManager cpaManager)
		{
			super(cpaManager);
		}

		@Override
		public void validate(EbMSBaseMessage message) throws ValidatorException
		{
		}
	}

	@NonNull
	CPAManager cpaManager;

	public static ClientCertificateValidator of(@NonNull CPAManager cpaManager, boolean enabled)
	{
		return enabled ? new ClientCertificateValidator(cpaManager) : new DisabledClientCertificateValidator(cpaManager);
	}

	public void validate(EbMSBaseMessage message) throws ValidatorException
	{
		val certificate = ClientCertificateManager.getCertificate();
		if (certificate != null)
		{
			if (!certificate.equals(getClientCertificate(message.getMessageHeader())))
				throw new ValidationException("Invalid SSL Client Certificate!");
		}
		else
			log.warn("No certificates found.");
	}

	private X509Certificate getClientCertificate(MessageHeader messageHeader)
	{
		try
		{
			val fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
			val service = CPAUtils.toString(messageHeader.getService());
			val deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction())
					.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
			return CPAUtils.getX509Certificate(CPAUtils.getClientCertificate(deliveryChannel));
		}
		catch (CertificateException e)
		{
			log.warn("",e);
			return null;
		}
	}

}
