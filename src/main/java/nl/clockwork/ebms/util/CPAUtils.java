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
package nl.clockwork.ebms.util;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.Duration;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.ToPartyInfo;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActionBindingType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Certificate;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationRole;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Packaging;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PerMessageCharacteristicsType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReceiverDigitalEnvelope;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SenderNonRepudiation;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Transport;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.w3._2000._09.xmldsig.X509DataType;

//TODO use JXPath
public class CPAUtils
{
	public static boolean equals(List<PartyId> cpaPartyIds, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> headerPartyIds)
	{
		return headerPartyIds.size() <= cpaPartyIds.size() && containsAll(cpaPartyIds,headerPartyIds);
	}

	public static String toString(PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	public static String toString(org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	public static String toString(ServiceType service)
	{
		return toString(service.getType(),service.getValue());
	}

	public static String toString(Service service)
	{
		return toString(service.getType(),service.getValue());
	}
	
	public static String toString(String type, String service)
	{
		return (type == null ? "" : type + ":") + service;
	}

	public static Service createEbMSMessageService()
	{
		Service result = new Service();
		result.setValue(Constants.EBMS_SERVICE_URI);
		return result;
	}
	
	public static List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> toPartyId(PartyId partyId)
	{
		List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> result = new ArrayList<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId>();
		org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId p = new org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId();
		p.setType(partyId.getType());
		p.setValue(partyId.getValue());
		result.add(p);
		return result;
	}

	public static FromPartyInfo getFromPartyInfo(PartyId partyId, CollaborationRole role, CanSend canSend)
	{
		FromPartyInfo result = new FromPartyInfo();
		result.setPartyIds(toPartyId(partyId));
		result.setRole(role.getRole().getName());
		result.setService(role.getServiceBinding().getService());
		result.setCanSend(canSend);
		return result;
	}

	public static ToPartyInfo getToPartyInfo(PartyId partyId, CollaborationRole role, CanReceive canReceive)
	{
		ToPartyInfo result = new ToPartyInfo();
		result.setPartyIds(toPartyId(partyId));
		result.setRole(role.getRole().getName());
		result.setService(role.getServiceBinding().getService());
		result.setCanReceive(canReceive);
		return result;
	}

	public static DeliveryChannel getDeliveryChannel(ActionBindingType bindingType)
	{
		return (DeliveryChannel)((JAXBElement<Object>)bindingType.getChannelId().get(0)).getValue();
	}
	
	public static DeliveryChannel getDeliveryChannel(List<JAXBElement<Object>> channelIds)
	{
		if (channelIds.size() > 0)
			return (DeliveryChannel)channelIds.get(0).getValue();
		else
			return null;
	}

	public static DocExchange getDocExchange(DeliveryChannel deliveryChannel)
	{
		return (DocExchange)deliveryChannel.getDocExchangeId();
	}
	
	public static Packaging getPackaging(CanSend canSend)
	{
		return (Packaging)canSend.getThisPartyActionBinding().getPackageId();
	}

	public static boolean isReliableMessaging(DeliveryChannel deliveryChannel)
	{
		return !PerMessageCharacteristicsType.NEVER.equals((deliveryChannel.getMessagingCharacteristics().getAckRequested())) /*&& ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding() != null && ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging() != null*/;
	}
	
	public static ReliableMessaging getReliableMessaging(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging();
	}

	public static Duration getPersistantDuration(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLReceiverBinding().getPersistDuration();
	}

	public static Duration getRetryInterval(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLReceiverBinding().getReliableMessaging().getRetryInterval();
	}
	
	public static Certificate getSigningCertificate(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef() != null)
			return (Certificate)docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef().getCertId();
		return null;
	}
	
	public static Certificate getEncryptionCertificate(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLReceiverBinding() != null && docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null && docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionCertificateRef() != null)
			return (Certificate)docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionCertificateRef().getCertId();
		return null;
	}
	
	public static String getNonRepudiationProtocol(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction() != null)
			return docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getNonRepudiationProtocol().getValue();
		return null;
	}

	public static String getHashFunction(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction() != null)
			return docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction();
		return null;
	}

	public static String getSignatureAlgorithm(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSignatureAlgorithm() != null && !docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSignatureAlgorithm().isEmpty())
		{
			SenderNonRepudiation senderNonRepudiation = docExchange.getEbXMLSenderBinding().getSenderNonRepudiation();
			return senderNonRepudiation.getSignatureAlgorithm().get(0).getW3C() != null ? senderNonRepudiation.getSignatureAlgorithm().get(0).getW3C() : getSignatureAlgorithm(senderNonRepudiation.getSignatureAlgorithm().get(0).getValue());
		}
		return null;
	}

	public static String getEncryptionAlgorithm(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLReceiverBinding() != null && docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null && docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionAlgorithm() != null && !docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionAlgorithm().isEmpty())
		{
			ReceiverDigitalEnvelope receiverDigitalEnvelope = docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope();
			return receiverDigitalEnvelope.getEncryptionAlgorithm().get(0).getW3C() != null ? receiverDigitalEnvelope.getEncryptionAlgorithm().get(0).getW3C() : getEncryptionAlgorithm(receiverDigitalEnvelope.getEncryptionAlgorithm().get(0).getValue());
		}
		return null;
	}

	public static String getUri(DeliveryChannel deliveryChannel)
	{
		Transport transport = (Transport)deliveryChannel.getTransportId();
		return transport.getTransportReceiver().getEndpoint().get(0).getUri();
	}
	
	public static String getHostname(DeliveryChannel deliveryChannel)
	{
		try
		{
			return deliveryChannel == null ? "hostname" : new URL(CPAUtils.getUri(deliveryChannel)).getHost();
		}
		catch (MalformedURLException e)
		{
			return "hostname";
		}
	}

	public static X509Certificate getX509Certificate(Certificate certificate) throws CertificateException
	{
		if (certificate != null)
			for (Object o : certificate.getKeyInfo().getContent())
				if (o instanceof JAXBElement<?> && ((JAXBElement<?>)o).getValue() instanceof X509DataType)
					for (Object p : ((X509DataType)((JAXBElement<?>)o).getValue()).getX509IssuerSerialOrX509SKIOrX509SubjectName())
						if (p instanceof JAXBElement<?> && "X509Certificate".equals(((JAXBElement<?>)p).getName().getLocalPart()))
							return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream((byte[])((JAXBElement<?>)p).getValue())); 
		return null;
	}

	private static boolean containsAll(List<PartyId> cpaPartyIds, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> headerPartyIds)
	{
		for (org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId headerPartyId : headerPartyIds)
			if (!contains(cpaPartyIds,headerPartyId))
				return false;
		return true;
	}

	private static boolean contains(List<PartyId> cpaPartyIds, org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId)
	{
		for (PartyId cpaPartyId : cpaPartyIds)
			if (equals(cpaPartyId,partyId))
				return true;
		return false;
	}

	private static boolean equals(PartyId cpaPartyId, org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId)
	{
		return partyId.getValue().equals(cpaPartyId.getValue()) && (partyId.getType() == null || (cpaPartyId.getType() != null && partyId.getType().equals(cpaPartyId.getType())));
	}
	
	private static String getSignatureAlgorithm(String value)
	{
		//TODO: Expected values include: RSA-MD5, RSA-SHA1, DSA-MD5, DSA-SHA1, SHA1withRSA, MD5withRSA, and so on.
		return value;
	}

	private static String getEncryptionAlgorithm(String value)
	{
		//TODO: Expected values include:
		return value;
	}

}
