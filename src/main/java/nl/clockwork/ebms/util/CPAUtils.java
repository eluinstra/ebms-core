/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.util;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBElement;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.cpp.cpa.ActionBindingType;
import nl.clockwork.ebms.model.cpp.cpa.CanReceive;
import nl.clockwork.ebms.model.cpp.cpa.CanSend;
import nl.clockwork.ebms.model.cpp.cpa.Certificate;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationRole;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.DocExchange;
import nl.clockwork.ebms.model.cpp.cpa.PartyId;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.cpp.cpa.ReceiverNonRepudiation;
import nl.clockwork.ebms.model.cpp.cpa.ReliableMessaging;
import nl.clockwork.ebms.model.cpp.cpa.SenderNonRepudiation;
import nl.clockwork.ebms.model.cpp.cpa.ServiceBinding;
import nl.clockwork.ebms.model.cpp.cpa.ServiceType;
import nl.clockwork.ebms.model.cpp.cpa.StatusValueType;
import nl.clockwork.ebms.model.cpp.cpa.X509DataType;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.Service;

//FIXME use JXPath
public class CPAUtils
{
	public static boolean isValid(CollaborationProtocolAgreement cpa, GregorianCalendar timestamp)
	{
		return StatusValueType.AGREED.equals(cpa.getStatus().getValue())
			&& timestamp.compareTo(cpa.getStart().toGregorianCalendar()) >= 0
			&& timestamp.compareTo(cpa.getEnd().toGregorianCalendar()) <= 0
		;
	}

	public static PartyInfo getPartyInfo(CollaborationProtocolAgreement cpa, List<nl.clockwork.ebms.model.ebxml.PartyId> partyIds)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
		{
			boolean found = true;
			for (nl.clockwork.ebms.model.ebxml.PartyId partyId : partyIds)
				for (PartyId cpaPartyId : partyInfo.getPartyId())
					found &= equals(partyId,cpaPartyId);
			if (found)
				return partyInfo;
		}
		return null;
	}
	
	private static boolean equals(nl.clockwork.ebms.model.ebxml.PartyId partyId, PartyId cpaPartyId)
	{
		return partyId.getType().equals(cpaPartyId.getType())
			&& partyId.getValue().equals(cpaPartyId.getValue());
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
		return serviceType.getType().equals(service.getType())
			&& serviceType.getValue().equals(service.getValue());
	}

	public static PartyInfo getSendingPartyInfo(CollaborationProtocolAgreement cpa, String from, String serviceType, String service, String action)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			for (CollaborationRole role : partyInfo.getCollaborationRole())
				if ((from == null || from.equals(role.getRole().getName()))
						&& (serviceType == null || serviceType.equals(role.getServiceBinding().getService().getType()))
						&& service.equals(role.getServiceBinding().getService().getValue())
				)
					for (CanSend canSend : role.getServiceBinding().getCanSend())
						if (action.equals(canSend.getThisPartyActionBinding().getAction()))
						{
							PartyInfo p = new PartyInfo();
							p.getPartyId().addAll(partyInfo.getPartyId());
							CollaborationRole cr = new CollaborationRole();
							cr.setRole(role.getRole());
							cr.setServiceBinding(new ServiceBinding());
							cr.getServiceBinding().setService(role.getServiceBinding().getService());
							cr.getServiceBinding().getCanSend().add(canSend);
							p.getCollaborationRole().add(cr);
							return p;
						}
		return null;
	}
	
	public static PartyInfo getReceivingPartyInfo(CollaborationProtocolAgreement cpa, String to, String serviceType, String service, String action)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			for (CollaborationRole role : partyInfo.getCollaborationRole())
				if ((to == null || to.equals(role.getRole().getName()))
						&& (serviceType == null || serviceType.equals(role.getServiceBinding().getService().getType()))
						&& service.equals(role.getServiceBinding().getService().getValue())
				)
					for (CanReceive canReceive : role.getServiceBinding().getCanReceive())
						if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
						{
							PartyInfo p = new PartyInfo();
							p.getPartyId().addAll(partyInfo.getPartyId());
							CollaborationRole r = new CollaborationRole();
							r.setRole(role.getRole());
							r.setServiceBinding(new ServiceBinding());
							r.getServiceBinding().setService(role.getServiceBinding().getService());
							r.getServiceBinding().getCanReceive().add(canReceive);
							p.getCollaborationRole().add(r);
							return p;
						}
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

	public static DeliveryChannel getDeliveryChannel(ActionBindingType bindingType)
	{
		return (DeliveryChannel)((JAXBElement<Object>)bindingType.getChannelId().get(0)).getValue();
	}
	
	public static List<DeliveryChannel> getSendingDeliveryChannels(PartyInfo partyInfo, String role, Service service, String action)
	{
		List<DeliveryChannel> result = new ArrayList<DeliveryChannel>();
		if (Constants.EBMS_SERVICE_URI.equals(service.getValue()))
		{
					result.add((DeliveryChannel)partyInfo.getDefaultMshChannelId());
		}
		else
		{
			ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
			if (serviceBinding != null)
				for (CanSend canSend : serviceBinding.getCanSend())
					if (action.equals(canSend.getThisPartyActionBinding().getAction()))
						for (JAXBElement<Object> o : canSend.getThisPartyActionBinding().getChannelId())
							result.add((DeliveryChannel)o.getValue());
		}
		return result;
	}
	
	public static List<DeliveryChannel> getReceivingDeliveryChannels(PartyInfo partyInfo, String role, Service service, String action)
	{
		List<DeliveryChannel> result = new ArrayList<DeliveryChannel>();
		if (Constants.EBMS_SERVICE_URI.equals(service.getValue()))
		{
					result.add((DeliveryChannel)partyInfo.getDefaultMshChannelId());
		}
		else
		{
			ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
			if (serviceBinding != null)
				for (CanReceive canReceive : serviceBinding.getCanReceive())
					if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
						for (JAXBElement<Object> o : canReceive.getThisPartyActionBinding().getChannelId())
							result.add((DeliveryChannel)o.getValue());
		}
		return result;
	}
	
	public static DocExchange getDocExchange(DeliveryChannel deliveryChannel)
	{
		return (DocExchange)deliveryChannel.getDocExchangeId();
	}
	
	public static Certificate getCertificate(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange != null && docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef() != null)
			return (Certificate)docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef().getCertId();
		return null;
	}
	
	public static SenderNonRepudiation getSenderNonRepudiation(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange != null && docExchange.getEbXMLReceiverBinding() != null)
			return docExchange.getEbXMLSenderBinding().getSenderNonRepudiation();
		return null;
	}
	
	public static ReceiverNonRepudiation getReceiverNonRepudiation(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange != null && docExchange.getEbXMLReceiverBinding() != null)
			return docExchange.getEbXMLReceiverBinding().getReceiverNonRepudiation();
		return null;
	}
	
	public static String getNonRepudiationProtocol(ReceiverNonRepudiation receiverNonRepudiation)
	{
		return receiverNonRepudiation.getNonRepudiationProtocol().getValue();
	}

	public static String getSignatureAlgorithm(ReceiverNonRepudiation receiverNonRepudiation)
	{
		return receiverNonRepudiation.getSignatureAlgorithm().get(0).getW3C() != null ? receiverNonRepudiation.getSignatureAlgorithm().get(0).getW3C() : receiverNonRepudiation.getSignatureAlgorithm().get(0).getValue();
	}

	public static String getHashFunction(ReceiverNonRepudiation receiverNonRepudiation)
	{
		return receiverNonRepudiation.getHashFunction();
	}

	public static X509Certificate getX509Certificate(Certificate certificate) throws CertificateException
	{
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

	public static ReliableMessaging getReliableMessaging(CollaborationProtocolAgreement cpa, MessageHeader messageHeader)
	{
		try
		{
			PartyInfo partyInfo = getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
			List<DeliveryChannel> deliveryChannels = getSendingDeliveryChannels(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
			return ((DocExchange)deliveryChannels.get(0).getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging();
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
}
