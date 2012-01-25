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
package nl.clockwork.mule.ebms.util;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.xml.security.signature.XMLSignature;

import nl.clockwork.mule.ebms.model.cpp.cpa.ActionBindingType;
import nl.clockwork.mule.ebms.model.cpp.cpa.CanReceive;
import nl.clockwork.mule.ebms.model.cpp.cpa.CanSend;
import nl.clockwork.mule.ebms.model.cpp.cpa.Certificate;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationRole;
import nl.clockwork.mule.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.mule.ebms.model.cpp.cpa.DocExchange;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyId;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.mule.ebms.model.cpp.cpa.ReliableMessaging;
import nl.clockwork.mule.ebms.model.cpp.cpa.ServiceBinding;
import nl.clockwork.mule.ebms.model.cpp.cpa.ServiceType;
import nl.clockwork.mule.ebms.model.cpp.cpa.SignatureAlgorithm;
import nl.clockwork.mule.ebms.model.cpp.cpa.X509DataType;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.Service;

//FIXME use JXPath
public class CPAUtils
{
	public static PartyInfo getPartyInfo(CollaborationProtocolAgreement cpa, List<nl.clockwork.mule.ebms.model.ebxml.PartyId> partyIds)
	{
		for (PartyInfo partyInfo : cpa.getPartyInfo())
		{
			boolean result = true;
			for (nl.clockwork.mule.ebms.model.ebxml.PartyId partyId : partyIds)
				for (PartyId cpaPartyId : partyInfo.getPartyId())
					result &= equals(partyId,cpaPartyId);
			if (result)
				return partyInfo;
		}
		return null;
	}
	
	private static boolean equals(nl.clockwork.mule.ebms.model.ebxml.PartyId partyId, PartyId cpaPartyId)
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
	
	public static List<DeliveryChannel> getDeliveryChannels(PartyInfo partyInfo, String role, Service service, String action)
	{
		List<DeliveryChannel> result = new ArrayList<DeliveryChannel>();
		ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
		if (serviceBinding != null)
			for (CanSend canSend : serviceBinding.getCanSend())
				if (action.equals(canSend.getThisPartyActionBinding().getAction()))
					for (JAXBElement<Object> o : canSend.getThisPartyActionBinding().getChannelId())
						result.add((DeliveryChannel)o.getValue());
		return result;
	}
	
	public static DocExchange getDocExchange(DeliveryChannel deliveryChannel)
	{
		return (DocExchange)deliveryChannel.getDocExchangeId();
	}
	
	public static Certificate getCertificate(DeliveryChannel deliveryChannel)
	{
		return (Certificate)getDocExchange(deliveryChannel).getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef().getCertId();
	}
	
	public static boolean isSigned(DeliveryChannel deliveryChannel)
	{
		DocExchange docExchange = getDocExchange(deliveryChannel);
		if (docExchange != null && docExchange.getEbXMLReceiverBinding() != null && docExchange.getEbXMLReceiverBinding().getReceiverNonRepudiation() != null && docExchange.getEbXMLReceiverBinding().getReceiverNonRepudiation().getSignatureAlgorithm() != null)
			for (SignatureAlgorithm algorithm : docExchange.getEbXMLReceiverBinding().getReceiverNonRepudiation().getSignatureAlgorithm())
				if (XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1.equals(algorithm.getValue()))
					return true;
		return false;
	}
	
	public static X509Certificate getX509Certificate(Certificate certificate) throws CertificateException
	{
		for (Object o : certificate.getKeyInfo().getContent())
		{
			if (o instanceof JAXBElement<?> && ((JAXBElement<Object>)o).getValue() instanceof X509DataType)
			{
				for (Object p : ((X509DataType)((JAXBElement<Object>)o).getValue()).getX509IssuerSerialOrX509SKIOrX509SubjectName())
				{
					if (p instanceof JAXBElement<?> && ((JAXBElement<Object>)p).getValue() instanceof byte[])
					{
						return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream((byte[])((JAXBElement<Object>)p).getValue())); 
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
			List<DeliveryChannel> deliveryChannels = getDeliveryChannels(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
			return ((DocExchange)deliveryChannels.get(0).getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging();
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
