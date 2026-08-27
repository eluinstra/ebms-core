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
package nl.clockwork.ebms;

import lombok.val;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActionBindingType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Certificate;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;

public class CPAUtils
{

	public static Service createEbMSMessageService()
	{
		val result = new Service();
		result.setValue(EbMSAction.EBMS_SERVICE_URI);
		return result;
	}

	public static DeliveryChannel getDeliveryChannel(ActionBindingType bindingType)
	{
		return (DeliveryChannel)bindingType.getChannelId().get(0).getValue();
	}

	public static ReliableMessaging getSenderReliableMessaging(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging();
	}

	public static DocExchange getDocExchange(DeliveryChannel deliveryChannel)
	{
		return (DocExchange)deliveryChannel.getDocExchangeId();
	}

	public static Certificate getSigningCertificate(DeliveryChannel deliveryChannel)
	{
		val docExchange = CPAUtils.getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef() != null)
			return (Certificate)docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef().getCertId();
		return null;
	}

	public static Certificate getEncryptionCertificate(DeliveryChannel deliveryChannel)
	{
		val docExchange = CPAUtils.getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLReceiverBinding() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionCertificateRef() != null)
			return (Certificate)docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionCertificateRef().getCertId();
		return null;
	}

	public static String getHashFunction(DeliveryChannel deliveryChannel)
	{
		val docExchange = CPAUtils.getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction() != null)
			return docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction();
		return null;
	}

	public static String getSignatureAlgorithm(DeliveryChannel deliveryChannel)
	{
		val docExchange = CPAUtils.getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSignatureAlgorithm() != null
				&& !docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSignatureAlgorithm().isEmpty())
		{
			val senderNonRepudiation = docExchange.getEbXMLSenderBinding().getSenderNonRepudiation();
			return senderNonRepudiation.getSignatureAlgorithm().get(0).getW3C() != null
					? senderNonRepudiation.getSignatureAlgorithm().get(0).getW3C()
					: senderNonRepudiation.getSignatureAlgorithm().get(0).getValue();
		}
		return null;
	}

	public static String getEncryptionAlgorithm(DeliveryChannel deliveryChannel)
	{
		val docExchange = CPAUtils.getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLReceiverBinding() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionAlgorithm() != null
				&& !docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionAlgorithm().isEmpty())
		{
			val receiverDigitalEnvelope = docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope();
			return receiverDigitalEnvelope.getEncryptionAlgorithm().get(0).getW3C() != null
					? receiverDigitalEnvelope.getEncryptionAlgorithm().get(0).getW3C()
					: receiverDigitalEnvelope.getEncryptionAlgorithm().get(0).getValue();
		}
		return null;
	}

}
