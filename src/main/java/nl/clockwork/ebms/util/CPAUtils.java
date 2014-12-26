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
import java.util.Calendar;
import java.util.List;

import javax.xml.bind.JAXBElement;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.Role;
import nl.clockwork.ebms.model.ToPartyInfo;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActionBindingType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Certificate;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationRole;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PerMessageCharacteristicsType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SenderNonRepudiation;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceBinding;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Transport;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.w3._2000._09.xmldsig.X509DataType;

//TODO use JXPath
public class CPAUtils
{
	public static boolean isValid(CollaborationProtocolAgreement cpa, EbMSMessage message)
	{
		Calendar timestamp = message.getMessageHeader().getMessageData().getTimestamp().toGregorianCalendar();
		return StatusValueType.AGREED.equals(cpa.getStatus().getValue())
				&& timestamp.compareTo(cpa.getStart().toGregorianCalendar()) >= 0
				&& timestamp.compareTo(cpa.getEnd().toGregorianCalendar()) <= 0;
	}

	public static String toString(PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	private static String toString(org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	public static String toString(ServiceType service)
	{
		return toString(service.getType(),service.getValue());
	}
	
	public static String toString(String type, String service)
	{
		return (type == null ? "" : type + ":") + service;
	}

	public static Party getFromParty(CollaborationProtocolAgreement cpa, Role fromRole, String service, String action)
	{
		String partyId = fromRole.getPartyId() == null ? toString(getFromPartyInfo(cpa,fromRole,service,action).getPartyId()) : fromRole.getPartyId();
		return new Party(partyId,fromRole.getRole());
	}
	
	public static Party getToParty(CollaborationProtocolAgreement cpa, Role toRole, String service, String action)
	{
		String partyId = toRole.getPartyId() == null ? toString(getToPartyInfo(cpa,toRole,service,action).getPartyId()) : toRole.getPartyId();
		return new Party(partyId,toRole.getRole());
	}
	
	public static String getUri(CollaborationProtocolAgreement cpa, EbMSMessage message)
	{
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,message.getMessageHeader().getTo().getPartyId());
		DeliveryChannel deliveryChannel = CPAUtils.getToDeliveryChannel(partyInfo,message.getMessageHeader().getTo().getRole(),message.getMessageHeader().getService(),message.getMessageHeader().getAction());
		return getUri(deliveryChannel);
	}

	public static String getResponseUri(CollaborationProtocolAgreement cpa, EbMSMessage message)
	{
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,message.getMessageHeader().getFrom().getPartyId());
		Service service = new Service();
		service.setValue(Constants.EBMS_SERVICE_URI);
		DeliveryChannel deliveryChannel = CPAUtils.getToDeliveryChannel(partyInfo,message.getMessageHeader().getFrom().getRole(),service,null);
		return getUri(deliveryChannel);
	}

	public static EbMSPartyInfo getMyPartyInfo(CollaborationProtocolAgreement cpa, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyIds)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			for (PartyId cpaPartyId : partyInfo.getPartyId())
				for (org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId : partyIds)
					if (equals(cpaPartyId,partyId))
					{
						EbMSPartyInfo result = new EbMSPartyInfo();
						result.setDefaultMshChannelId((DeliveryChannel)partyInfo.getDefaultMshChannelId());
						result.setPartyId(getPartyId(partyInfo.getPartyId(),toString(partyIds.get(0))));
						return result;
					}
		return null;
	}
	
	public static EbMSPartyInfo getMyPartyInfo(CollaborationProtocolAgreement cpa, Party party)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (party.matches(partyInfo.getPartyId()))
				for (CollaborationRole role : partyInfo.getCollaborationRole())
					if (party.matches(role.getRole()))
					{
						EbMSPartyInfo result = new EbMSPartyInfo();
						result.setDefaultMshChannelId((DeliveryChannel)partyInfo.getDefaultMshChannelId());
						result.setPartyId(getPartyId(partyInfo.getPartyId(),party.getPartyId()));
						result.setRole(party.getRole());
						return result;
					}
		return null;
	}

	public static PartyId getPartyId(List<PartyId> partyIds, String partyId)
	{
		for (PartyId p : partyIds)
			if (CPAUtils.toString(p).equals(partyId))
				return p;
		return null;
	}
	
	public static PartyInfo getPartyInfo(CollaborationProtocolAgreement cpa, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyIds)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			for (PartyId cpaPartyId : partyInfo.getPartyId())
				for (org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId : partyIds)
					if (equals(cpaPartyId,partyId))
						return partyInfo;
		return null;
	}
	
	private static boolean equals(PartyId cpaPartyId, org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId)
	{
		return partyId.getType().equals(cpaPartyId.getType()) && partyId.getValue().equals(cpaPartyId.getValue());
	}
	
	public static ServiceBinding getServiceBinding(PartyInfo partyInfo, String role, Service service)
	{
		for (CollaborationRole collaborationRole : partyInfo.getCollaborationRole())
			if (role.equals(collaborationRole.getRole().getName()) && equals(collaborationRole.getServiceBinding().getService(),service))
				return collaborationRole.getServiceBinding();
		return null;
	}

	private static boolean equals(ServiceType serviceType, Service service)
	{
		return serviceType.getType().equals(service.getType()) && serviceType.getValue().equals(service.getValue());
	}

	public static FromPartyInfo getFromPartyInfo(CollaborationProtocolAgreement cpa, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyIds, String fromRole, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpa,partyIds);
		for (CollaborationRole role : partyInfo.getCollaborationRole())
			if (fromRole.equals(role.getRole().getName()) && service.equals(toString(role.getServiceBinding().getService())))
				for (CanSend canSend : role.getServiceBinding().getCanSend())
					if (action.equals(canSend.getThisPartyActionBinding().getAction()))
						return getFromPartyInfo(partyInfo,role,canSend);
		return null;
	}
	
	public static FromPartyInfo getFromPartyInfo(CollaborationProtocolAgreement cpa, Role fromRole, String service, String action)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (fromRole == null || fromRole.matches(partyInfo.getPartyId()))
				for (CollaborationRole role : partyInfo.getCollaborationRole())
					if (fromRole == null || fromRole.matches(role.getRole()) && service.equals(toString(role.getServiceBinding().getService())))
						for (CanSend canSend : role.getServiceBinding().getCanSend())
							if (action.equals(canSend.getThisPartyActionBinding().getAction()))
								return getFromPartyInfo(partyInfo,role,canSend);
		return null;
	}
	
	public static ToPartyInfo getToPartyInfo(CollaborationProtocolAgreement cpa, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyIds, String toRole, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpa,partyIds);
		for (CollaborationRole role : partyInfo.getCollaborationRole())
			if (toRole.equals(role.getRole().getName()) && service.equals(toString(role.getServiceBinding().getService())))
				for (CanReceive canReceive : role.getServiceBinding().getCanReceive())
					if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
						return getToPartyInfo(partyInfo,role,canReceive);
		return null;
	}
	
	public static ToPartyInfo getToPartyInfo(CollaborationProtocolAgreement cpa, Role toRole, String service, String action)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (toRole == null || toRole.matches(partyInfo.getPartyId()))
				for (CollaborationRole role : partyInfo.getCollaborationRole())
					if (toRole == null || toRole.matches(role.getRole()) && service.equals(toString(role.getServiceBinding().getService())))
						for (CanReceive canReceive : role.getServiceBinding().getCanReceive())
							if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
								return getToPartyInfo(partyInfo,role,canReceive);
		return null;
	}

	public static CanSend getCanSend(PartyInfo partyInfo, String role, Service service, String action)
	{
		ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
		if (serviceBinding != null)
			for (CanSend canSend : serviceBinding.getCanSend())
				if (action.equals(canSend.getThisPartyActionBinding().getAction()))
					return canSend;
		return null;
	}

	public static CanReceive getCanReceive(PartyInfo partyInfo, String role, Service service, String action)
	{
		ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
		if (serviceBinding != null)
			for (CanReceive canReceive : serviceBinding.getCanReceive())
				if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
					return canReceive;
		return null;
	}

	public static boolean canSend(PartyInfo partyInfo, String role, Service service, String action)
	{
		return getCanSend(partyInfo,role,service,action) != null;
	}

	public static boolean canReceive(PartyInfo partyInfo, String role, Service service, String action)
	{
		return getCanReceive(partyInfo,role,service,action) != null;
	}

	public static ToPartyInfo getToPartyInfo(CollaborationProtocolAgreement cpa, ActionBindingType actionBinding)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			for (CollaborationRole role : partyInfo.getCollaborationRole())
				for (CanReceive canReceive : role.getServiceBinding().getCanReceive())
					if (canReceive.getThisPartyActionBinding().equals(actionBinding))
						return getToPartyInfo(partyInfo,role,canReceive);
		return null;
	}

	private static FromPartyInfo getFromPartyInfo(PartyInfo partyInfo, CollaborationRole role, CanSend canSend)
	{
		FromPartyInfo result = new FromPartyInfo();
		result.setDefaultMshChannelId((DeliveryChannel)partyInfo.getDefaultMshChannelId());
		result.setPartyId(partyInfo.getPartyId().get(0));
		result.setRole(role.getRole().getName());
		result.setService(role.getServiceBinding().getService());
		result.setCanSend(canSend);
		return result;
	}

	private static ToPartyInfo getToPartyInfo(PartyInfo partyInfo, CollaborationRole role, CanReceive canReceive)
	{
		ToPartyInfo result = new ToPartyInfo();
		result.setDefaultMshChannelId((DeliveryChannel)partyInfo.getDefaultMshChannelId());
		result.setPartyId(partyInfo.getPartyId().get(0));
		result.setRole(role.getRole().getName());
		result.setService(role.getServiceBinding().getService());
		result.setCanReceive(canReceive);
		return result;
	}

	public static DeliveryChannel getFromDeliveryChannel(PartyInfo partyInfo, String role, Service service, String action)
	{
		if (Constants.EBMS_SERVICE_URI.equals(service.getValue()))
			return (DeliveryChannel)partyInfo.getDefaultMshChannelId();
		else
		{
			ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
			if (serviceBinding != null)
				for (CanSend canSend : serviceBinding.getCanSend())
					if (action.equals(canSend.getThisPartyActionBinding().getAction()))
						return getDeliveryChannel(canSend.getThisPartyActionBinding().getChannelId());
		}
		return null;
	}
	
	public static DeliveryChannel getToDeliveryChannel(PartyInfo partyInfo, String role, Service service, String action)
	{
		if (Constants.EBMS_SERVICE_URI.equals(service.getValue()))
			return (DeliveryChannel)partyInfo.getDefaultMshChannelId();
		else
		{
			ServiceBinding serviceBinding = getServiceBinding(partyInfo,role,service);
			if (serviceBinding != null)
				for (CanReceive canReceive : serviceBinding.getCanReceive())
					if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
						return getDeliveryChannel(canReceive.getThisPartyActionBinding().getChannelId());
		}
		return null;
	}
	
	public static DeliveryChannel getDeliveryChannel(List<JAXBElement<Object>> channelIds)
	{
		if (channelIds.size() > 0)
			return (DeliveryChannel)channelIds.get(0).getValue();
		else
			return null;
	}

	public static boolean isSigned(PartyInfo partyInfo, String role, Service service, String action)
	{
		CanSend canSend = getCanSend(partyInfo,role,service,action);
		//DocExchange docExchange = getDocExchange(getFromDeliveryChannel(partyInfo,role,service,action));
		return canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsNonRepudiationRequired() /*&& docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null*/;
	}

	public static DeliveryChannel getDefaultDeliveryChannel(PartyInfo partyInfo)
	{
		return (DeliveryChannel)partyInfo.getDefaultMshChannelId();
	}

	public static DeliveryChannel getDeliveryChannel(ActionBindingType bindingType)
	{
		return (DeliveryChannel)((JAXBElement<Object>)bindingType.getChannelId().get(0)).getValue();
	}
	
	public static DocExchange getDocExchange(DeliveryChannel deliveryChannel)
	{
		return (DocExchange)deliveryChannel.getDocExchangeId();
	}
	
	public static boolean isReliableMessaging(CollaborationProtocolAgreement cpa, DeliveryChannel deliveryChannel)
	{
		return !PerMessageCharacteristicsType.NEVER.equals((deliveryChannel.getMessagingCharacteristics().getAckRequested())) /*&& ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding() != null && ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging() != null*/;
	}
	
	public static ReliableMessaging getReliableMessaging(CollaborationProtocolAgreement cpa, DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging();
	}
	
	public static Certificate getSigningCertificate(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		//if (docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef() != null)
			return (Certificate)docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef().getCertId();
		//return null;
	}
	
	public static String getNonRepudiationProtocol(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		//if (docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction() != null)
			return docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getNonRepudiationProtocol().getValue();
		//return null;
	}

	public static String getHashFunction(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		//if (docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction() != null)
		{
			SenderNonRepudiation senderNonRepudiation = docExchange.getEbXMLSenderBinding().getSenderNonRepudiation();
			return senderNonRepudiation.getHashFunction();
		}
		//return null;
	}

	public static String getSignatureAlgorithm(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		//if (docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSignatureAlgorithm() != null && !docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSignatureAlgorithm().isEmpty())
		{
			SenderNonRepudiation senderNonRepudiation = docExchange.getEbXMLSenderBinding().getSenderNonRepudiation();
			return senderNonRepudiation.getSignatureAlgorithm().get(0).getW3C() != null ? senderNonRepudiation.getSignatureAlgorithm().get(0).getW3C() : getSignatureAlgorithm(senderNonRepudiation.getSignatureAlgorithm().get(0).getValue());
		}
		//return null;
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
			String uri = CPAUtils.getUri(deliveryChannel);
			URL url = new URL(uri);
			return url.getHost();
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
			{
				if (o instanceof JAXBElement<?> && ((JAXBElement<?>)o).getValue() instanceof X509DataType)
				{
					for (Object p : ((X509DataType)((JAXBElement<?>)o).getValue()).getX509IssuerSerialOrX509SKIOrX509SubjectName())
					{
						if (p instanceof JAXBElement<?> && "X509Certificate".equals(((JAXBElement<?>)p).getName().getLocalPart()))
						{
							return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream((byte[])((JAXBElement<?>)p).getValue())); 
						}
					}
				}
			}
		return null;
	}

	private static String getSignatureAlgorithm(String value)
	{
		//TODO: Expected values include: RSA-MD5, RSA-SHA1, DSA-MD5, DSA-SHA1, SHA1withRSA, MD5withRSA, and so on.
		return value;
	}

}
