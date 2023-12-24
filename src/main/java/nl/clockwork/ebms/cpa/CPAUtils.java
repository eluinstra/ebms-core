/*
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
package nl.clockwork.ebms.cpa;

import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageUtils;
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
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Transport;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.w3._2000._09.xmldsig.X509DataType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CPAUtils
{
	public static boolean equals(List<PartyId> cpaPartyIds, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> headerPartyIds)
	{
		return !headerPartyIds.isEmpty() && headerPartyIds.size() <= cpaPartyIds.size() && containsAll(cpaPartyIds, headerPartyIds);
	}

	public static String toString(PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	public static String toString(org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	public static String toString(List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyId)
	{
		return partyId.stream().map(CPAUtils::toString).collect(Collectors.joining(","));
	}

	public static String toString(ServiceType service)
	{
		return toString(service.getType(), service.getValue());
	}

	public static String toString(Service service)
	{
		return toString(service.getType(), service.getValue());
	}

	public static String toString(String type, String service)
	{
		return (type == null ? "" : type + ":") + service;
	}

	public static Service createEbMSMessageService()
	{
		val result = new Service();
		result.setValue(EbMSAction.EBMS_SERVICE_URI);
		return result;
	}

	public static List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> toPartyId(PartyId partyId)
	{
		val result = new ArrayList<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId>();
		result.add(partyId(partyId));
		return result;
	}

	private static org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId partyId(PartyId partyId)
	{
		val result = new org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId();
		result.setType(partyId.getType());
		result.setValue(partyId.getValue());
		return result;
	}

	public static FromPartyInfo getFromPartyInfo(PartyId partyId, CollaborationRole role, CanSend canSend)
	{
		return FromPartyInfo.builder()
				.partyIds(toPartyId(partyId))
				.role(role.getRole().getName())
				.service(role.getServiceBinding().getService())
				.canSend(canSend)
				.build();
	}

	public static ToPartyInfo getToPartyInfo(PartyId partyId, CollaborationRole role, CanReceive canReceive)
	{
		return ToPartyInfo.builder()
				.partyIds(toPartyId(partyId))
				.role(role.getRole().getName())
				.service(role.getServiceBinding().getService())
				.canReceive(canReceive)
				.build();
	}

	public static DeliveryChannel getDeliveryChannel(ActionBindingType bindingType)
	{
		return (DeliveryChannel)bindingType.getChannelId().get(0).getValue();
	}

	public static DeliveryChannel getDeliveryChannel(List<JAXBElement<Object>> channelIds)
	{
		if (!channelIds.isEmpty())
			return (DeliveryChannel)channelIds.get(0).getValue();
		else
			return null;
	}

	private static Transport getTransport(DeliveryChannel deliveryChannel)
	{
		return (Transport)deliveryChannel.getTransportId();
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
		return !PerMessageCharacteristicsType.NEVER.equals((deliveryChannel.getMessagingCharacteristics().getAckRequested()));
		// && ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding() != null
		// && ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging() != null
	}

	public static ReliableMessaging getSenderReliableMessaging(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging();
	}

	public static ReliableMessaging getReceiverReliableMessaging(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLReceiverBinding().getReliableMessaging();
	}

	public static Duration getPersistantDuration(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLReceiverBinding().getPersistDuration();
	}

	public static Instant getPersistTime(Instant timestamp, DeliveryChannel deliveryChannel)
	{
		val persistDuration = CPAUtils.getDocExchange(deliveryChannel).getEbXMLReceiverBinding().getPersistDuration();
		if (persistDuration != null)
		{
			val persistTime = Instant.from(timestamp);
			return persistTime.plus(persistDuration);
		}
		else
			return null;
	}

	public static Duration getRetryInterval(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLReceiverBinding().getReliableMessaging().getRetryInterval();
	}

	public static Certificate getClientCertificate(DeliveryChannel deliveryChannel)
	{
		val transport = getTransport(deliveryChannel);
		if (transport.getTransportSender().getTransportClientSecurity() != null
				&& transport.getTransportSender().getTransportClientSecurity().getClientCertificateRef() != null)
			return (Certificate)transport.getTransportSender().getTransportClientSecurity().getClientCertificateRef().getCertId();
		return null;
	}

	public static Certificate getSigningCertificate(DeliveryChannel deliveryChannel)
	{
		val docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef() != null)
			return (Certificate)docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef().getCertId();
		return null;
	}

	public static Certificate getEncryptionCertificate(DeliveryChannel deliveryChannel)
	{
		val docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLReceiverBinding() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionCertificateRef() != null)
			return (Certificate)docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionCertificateRef().getCertId();
		return null;
	}

	public static String getNonRepudiationProtocol(DeliveryChannel deliveryChannel)
	{
		val docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction() != null)
			return docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getNonRepudiationProtocol().getValue();
		return null;
	}

	public static String getHashFunction(DeliveryChannel deliveryChannel)
	{
		val docExchange = getDocExchange(deliveryChannel);
		if (docExchange.getEbXMLSenderBinding() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction() != null)
			return docExchange.getEbXMLSenderBinding().getSenderNonRepudiation().getHashFunction();
		return null;
	}

	public static String getSignatureAlgorithm(DeliveryChannel deliveryChannel)
	{
		val docExchange = getDocExchange(deliveryChannel);
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
		val docExchange = getDocExchange(deliveryChannel);
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

	public static String getUri(DeliveryChannel deliveryChannel)
	{
		if (deliveryChannel != null)
		{
			val transport = (Transport)deliveryChannel.getTransportId();
			if (transport != null && transport.getTransportReceiver() != null)
				return transport.getTransportReceiver().getEndpoint().get(0).getUri();
		}
		return null;
	}

	public static String getHostname(DeliveryChannel deliveryChannel)
	{
		try
		{
			return new URL(CPAUtils.getUri(deliveryChannel)).getHost();
		}
		catch (MalformedURLException e)
		{
			return "";
		}
	}

	private static Predicate<Object> isJaxbElement = o -> o instanceof JAXBElement<?>;
	private static Predicate<Object> isX509DataType = o -> ((JAXBElement<?>)o).getValue() instanceof X509DataType;
	private static Predicate<Object> isX509Certificate = o -> "X509Certificate".equals(((JAXBElement<?>)o).getName().getLocalPart());
	private static Function<Object, List<Object>> toX509IssuerSerialOrX509SKIOrX509SubjectName =
			o -> ((X509DataType)((JAXBElement<?>)o).getValue()).getX509IssuerSerialOrX509SKIOrX509SubjectName();
	private static Function<Object, byte[]> toByteArray = o -> (byte[])((JAXBElement<?>)o).getValue();

	public static X509Certificate getX509Certificate(Certificate certificate)
	{
		return Optional.ofNullable(certificate)
				.flatMap(
						c -> c.getKeyInfo()
								.getContent()
								.stream()
								.filter(isJaxbElement.and(isX509DataType))
								.map(toX509IssuerSerialOrX509SKIOrX509SubjectName)
								.flatMap(
										l -> l.stream().filter(isJaxbElement.and(isX509Certificate)).map(toByteArray).map(ByteArrayInputStream::new).map(CPAUtils::toCertificate))
								.findFirst())
				.orElse(null);
	}

	private static X509Certificate toCertificate(InputStream s)
	{
		return Try.of(() -> (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(s)).getOrElseThrow(e -> new IllegalStateException(e));
	}

	private static boolean containsAll(List<PartyId> cpaPartyIds, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> headerPartyIds)
	{
		return headerPartyIds.stream().map(EbMSMessageUtils::toString).allMatch(anyMatchIn(cpaPartyIds));
	}

	private static Predicate<String> anyMatchIn(List<PartyId> cpaPartyIds)
	{
		return headerPartyId -> cpaPartyIds.stream().map(CPAUtils::toString).anyMatch(headerPartyId::equals);
	}

}
