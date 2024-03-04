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
package org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlRegistry;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Duration;
import java.time.Instant;
import javax.xml.namespace.QName;
import nl.clockwork.ebms.jaxb.DurationAdapter;
import nl.clockwork.ebms.jaxb.InstantAdapter;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0 package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content. The Java representation of XML content can
 * consist of schema derived interfaces and classes representing the binding of schema type definitions, element declarations and model groups. Factory methods
 * for each of these are provided in this class.
 */
@XmlRegistry
public class ObjectFactory
{

	private final static QName _AccessAuthentication_QNAME =
			new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "AccessAuthentication");
	private final static QName _SendingProtocol_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "SendingProtocol");
	private final static QName _Service_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "Service");
	private final static QName _Type_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "Type");
	private final static QName _PersistDuration_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "PersistDuration");
	private final static QName _ChannelId_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "ChannelId");
	private final static QName _SecurityDetailsRef_QNAME =
			new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "SecurityDetailsRef");
	private final static QName _ReceivingProtocol_QNAME =
			new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "ReceivingProtocol");
	private final static QName _Protocol_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "Protocol");
	private final static QName _End_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "End");
	private final static QName _HashFunction_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "HashFunction");
	private final static QName _Start_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", "Start");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
	 * org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0
	 */
	public ObjectFactory()
	{
	}

	/**
	 * Create an instance of {@link SignatureAlgorithm }
	 */
	public SignatureAlgorithm createSignatureAlgorithm()
	{
		return new SignatureAlgorithm();
	}

	/**
	 * Create an instance of {@link SecurityDetailsRefType }
	 */
	public SecurityDetailsRefType createSecurityDetailsRefType()
	{
		return new SecurityDetailsRefType();
	}

	/**
	 * Create an instance of {@link Certificate }
	 */
	public Certificate createCertificate()
	{
		return new Certificate();
	}

	/**
	 * Create an instance of {@link TransportReceiver }
	 */
	public TransportReceiver createTransportReceiver()
	{
		return new TransportReceiver();
	}

	/**
	 * Create an instance of {@link DocExchange }
	 */
	public DocExchange createDocExchange()
	{
		return new DocExchange();
	}

	/**
	 * Create an instance of {@link Transport }
	 */
	public Transport createTransport()
	{
		return new Transport();
	}

	/**
	 * Create an instance of {@link TransportServerSecurity }
	 */
	public TransportServerSecurity createTransportServerSecurity()
	{
		return new TransportServerSecurity();
	}

	/**
	 * Create an instance of {@link ProcessSpecification }
	 */
	public ProcessSpecification createProcessSpecification()
	{
		return new ProcessSpecification();
	}

	/**
	 * Create an instance of {@link CollaborationProtocolProfile }
	 */
	public CollaborationProtocolProfile createCollaborationProtocolProfile()
	{
		return new CollaborationProtocolProfile();
	}

	/**
	 * Create an instance of {@link SenderNonRepudiation }
	 */
	public SenderNonRepudiation createSenderNonRepudiation()
	{
		return new SenderNonRepudiation();
	}

	/**
	 * Create an instance of {@link Packaging.CompositeList }
	 */
	public Packaging.CompositeList createPackagingCompositeList()
	{
		return new Packaging.CompositeList();
	}

	/**
	 * Create an instance of {@link ConversationConstraints }
	 */
	public ConversationConstraints createConversationConstraints()
	{
		return new ConversationConstraints();
	}

	/**
	 * Create an instance of {@link EbXMLSenderBinding }
	 */
	public EbXMLSenderBinding createEbXMLSenderBinding()
	{
		return new EbXMLSenderBinding();
	}

	/**
	 * Create an instance of {@link Packaging.CompositeList.Encapsulation }
	 */
	public Packaging.CompositeList.Encapsulation createPackagingCompositeListEncapsulation()
	{
		return new Packaging.CompositeList.Encapsulation();
	}

	/**
	 * Create an instance of {@link EbXMLReceiverBinding }
	 */
	public EbXMLReceiverBinding createEbXMLReceiverBinding()
	{
		return new EbXMLReceiverBinding();
	}

	/**
	 * Create an instance of {@link NamespaceSupported }
	 */
	public NamespaceSupported createNamespaceSupported()
	{
		return new NamespaceSupported();
	}

	/**
	 * Create an instance of {@link TrustAnchors }
	 */
	public TrustAnchors createTrustAnchors()
	{
		return new TrustAnchors();
	}

	/**
	 * Create an instance of {@link CollaborationRole }
	 */
	public CollaborationRole createCollaborationRole()
	{
		return new CollaborationRole();
	}

	/**
	 * Create an instance of {@link Packaging.ProcessingCapabilities }
	 */
	public Packaging.ProcessingCapabilities createPackagingProcessingCapabilities()
	{
		return new Packaging.ProcessingCapabilities();
	}

	/**
	 * Create an instance of {@link PartyRef }
	 */
	public PartyRef createPartyRef()
	{
		return new PartyRef();
	}

	/**
	 * Create an instance of {@link ReliableMessaging }
	 */
	public ReliableMessaging createReliableMessaging()
	{
		return new ReliableMessaging();
	}

	/**
	 * Create an instance of {@link TransportClientSecurity }
	 */
	public TransportClientSecurity createTransportClientSecurity()
	{
		return new TransportClientSecurity();
	}

	/**
	 * Create an instance of {@link SecurityPolicyType }
	 */
	public SecurityPolicyType createSecurityPolicyType()
	{
		return new SecurityPolicyType();
	}

	/**
	 * Create an instance of {@link Status }
	 */
	public Status createStatus()
	{
		return new Status();
	}

	/**
	 * Create an instance of {@link Endpoint }
	 */
	public Endpoint createEndpoint()
	{
		return new Endpoint();
	}

	/**
	 * Create an instance of {@link ActionBindingType }
	 */
	public ActionBindingType createActionBindingType()
	{
		return new ActionBindingType();
	}

	/**
	 * Create an instance of {@link ReceiverDigitalEnvelope }
	 */
	public ReceiverDigitalEnvelope createReceiverDigitalEnvelope()
	{
		return new ReceiverDigitalEnvelope();
	}

	/**
	 * Create an instance of {@link Signature }
	 */
	public Signature createSignature()
	{
		return new Signature();
	}

	/**
	 * Create an instance of {@link CanSend }
	 */
	public CanSend createCanSend()
	{
		return new CanSend();
	}

	/**
	 * Create an instance of {@link PartyInfo }
	 */
	public PartyInfo createPartyInfo()
	{
		return new PartyInfo();
	}

	/**
	 * Create an instance of {@link MessagingCharacteristics }
	 */
	public MessagingCharacteristics createMessagingCharacteristics()
	{
		return new MessagingCharacteristics();
	}

	/**
	 * Create an instance of {@link SecurityDetails }
	 */
	public SecurityDetails createSecurityDetails()
	{
		return new SecurityDetails();
	}

	/**
	 * Create an instance of {@link CollaborationProtocolAgreement }
	 */
	public CollaborationProtocolAgreement createCollaborationProtocolAgreement()
	{
		return new CollaborationProtocolAgreement();
	}

	/**
	 * Create an instance of {@link TransportSender }
	 */
	public TransportSender createTransportSender()
	{
		return new TransportSender();
	}

	/**
	 * Create an instance of {@link PartyId }
	 */
	public PartyId createPartyId()
	{
		return new PartyId();
	}

	/**
	 * Create an instance of {@link Role }
	 */
	public Role createRole()
	{
		return new Role();
	}

	/**
	 * Create an instance of {@link EncryptionAlgorithm }
	 */
	public EncryptionAlgorithm createEncryptionAlgorithm()
	{
		return new EncryptionAlgorithm();
	}

	/**
	 * Create an instance of {@link Comment }
	 */
	public Comment createComment()
	{
		return new Comment();
	}

	/**
	 * Create an instance of {@link SenderDigitalEnvelope }
	 */
	public SenderDigitalEnvelope createSenderDigitalEnvelope()
	{
		return new SenderDigitalEnvelope();
	}

	/**
	 * Create an instance of {@link SecurityPolicy }
	 */
	public SecurityPolicy createSecurityPolicy()
	{
		return new SecurityPolicy();
	}

	/**
	 * Create an instance of {@link CollaborationActivity }
	 */
	public CollaborationActivity createCollaborationActivity()
	{
		return new CollaborationActivity();
	}

	/**
	 * Create an instance of {@link SimplePart }
	 */
	public SimplePart createSimplePart()
	{
		return new SimplePart();
	}

	/**
	 * Create an instance of {@link CertificateRefType }
	 */
	public CertificateRefType createCertificateRefType()
	{
		return new CertificateRefType();
	}

	/**
	 * Create an instance of {@link DeliveryChannel }
	 */
	public DeliveryChannel createDeliveryChannel()
	{
		return new DeliveryChannel();
	}

	/**
	 * Create an instance of {@link ProtocolType }
	 */
	public ProtocolType createProtocolType()
	{
		return new ProtocolType();
	}

	/**
	 * Create an instance of {@link BusinessTransactionCharacteristics }
	 */
	public BusinessTransactionCharacteristics createBusinessTransactionCharacteristics()
	{
		return new BusinessTransactionCharacteristics();
	}

	/**
	 * Create an instance of {@link ReceiverNonRepudiation }
	 */
	public ReceiverNonRepudiation createReceiverNonRepudiation()
	{
		return new ReceiverNonRepudiation();
	}

	/**
	 * Create an instance of {@link ServiceBinding }
	 */
	public ServiceBinding createServiceBinding()
	{
		return new ServiceBinding();
	}

	/**
	 * Create an instance of {@link Packaging }
	 */
	public Packaging createPackaging()
	{
		return new Packaging();
	}

	/**
	 * Create an instance of {@link EncryptionTransforms }
	 */
	public EncryptionTransforms createEncryptionTransforms()
	{
		return new EncryptionTransforms();
	}

	/**
	 * Create an instance of {@link ActionContext }
	 */
	public ActionContext createActionContext()
	{
		return new ActionContext();
	}

	/**
	 * Create an instance of {@link CanReceive }
	 */
	public CanReceive createCanReceive()
	{
		return new CanReceive();
	}

	/**
	 * Create an instance of {@link OverrideMshActionBinding }
	 */
	public OverrideMshActionBinding createOverrideMshActionBinding()
	{
		return new OverrideMshActionBinding();
	}

	/**
	 * Create an instance of {@link SignatureTransforms }
	 */
	public SignatureTransforms createSignatureTransforms()
	{
		return new SignatureTransforms();
	}

	/**
	 * Create an instance of {@link ServiceType }
	 */
	public ServiceType createServiceType()
	{
		return new ServiceType();
	}

	/**
	 * Create an instance of {@link Packaging.CompositeList.Composite }
	 */
	public Packaging.CompositeList.Composite createPackagingCompositeListComposite()
	{
		return new Packaging.CompositeList.Composite();
	}

	/**
	 * Create an instance of {@link Constituent }
	 */
	public Constituent createConstituent()
	{
		return new Constituent();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link AccessAuthenticationType }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "AccessAuthentication")
	public JAXBElement<AccessAuthenticationType> createAccessAuthentication(AccessAuthenticationType value)
	{
		return new JAXBElement<AccessAuthenticationType>(_AccessAuthentication_QNAME, AccessAuthenticationType.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ProtocolType }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "SendingProtocol")
	public JAXBElement<ProtocolType> createSendingProtocol(ProtocolType value)
	{
		return new JAXBElement<ProtocolType>(_SendingProtocol_QNAME, ProtocolType.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ServiceType }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "Service")
	public JAXBElement<ServiceType> createService(ServiceType value)
	{
		return new JAXBElement<ServiceType>(_Service_QNAME, ServiceType.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "Type")
	public JAXBElement<String> createType(String value)
	{
		return new JAXBElement<String>(_Type_QNAME, String.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Duration }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "PersistDuration")
	@XmlJavaTypeAdapter(DurationAdapter.class)
	public JAXBElement<Duration> createPersistDuration(Duration value)
	{
		return new JAXBElement<Duration>(_PersistDuration_QNAME, Duration.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "ChannelId")
	@XmlIDREF
	public JAXBElement<Object> createChannelId(Object value)
	{
		return new JAXBElement<Object>(_ChannelId_QNAME, Object.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link SecurityDetailsRefType }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "SecurityDetailsRef")
	public JAXBElement<SecurityDetailsRefType> createSecurityDetailsRef(SecurityDetailsRefType value)
	{
		return new JAXBElement<SecurityDetailsRefType>(_SecurityDetailsRef_QNAME, SecurityDetailsRefType.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ProtocolType }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "ReceivingProtocol")
	public JAXBElement<ProtocolType> createReceivingProtocol(ProtocolType value)
	{
		return new JAXBElement<ProtocolType>(_ReceivingProtocol_QNAME, ProtocolType.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ProtocolType }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "Protocol")
	public JAXBElement<ProtocolType> createProtocol(ProtocolType value)
	{
		return new JAXBElement<ProtocolType>(_Protocol_QNAME, ProtocolType.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Instant }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "End")
	@XmlJavaTypeAdapter(InstantAdapter.class)
	public JAXBElement<Instant> createEnd(Instant value)
	{
		return new JAXBElement<Instant>(_End_QNAME, Instant.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "HashFunction")
	public JAXBElement<String> createHashFunction(String value)
	{
		return new JAXBElement<String>(_HashFunction_QNAME, String.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Instant }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", name = "Start")
	@XmlJavaTypeAdapter(InstantAdapter.class)
	public JAXBElement<Instant> createStart(Instant value)
	{
		return new JAXBElement<Instant>(_Start_QNAME, Instant.class, null, value);
	}

}
