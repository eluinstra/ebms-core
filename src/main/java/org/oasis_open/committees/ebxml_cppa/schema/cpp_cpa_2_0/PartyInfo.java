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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}PartyId" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}PartyRef" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}CollaborationRole" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Certificate" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}SecurityDetails" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}DeliveryChannel" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Transport" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}DocExchange" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}OverrideMshActionBinding" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="partyName" use="required" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}non-empty-string" />
 *       &lt;attribute name="defaultMshChannelId" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="defaultMshPackageId" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
		name = "",
		propOrder = {"partyId", "partyRef", "collaborationRole", "certificate", "securityDetails", "deliveryChannel", "transport", "docExchange",
				"overrideMshActionBinding"})
@XmlRootElement(name = "PartyInfo")
public class PartyInfo implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "PartyId", required = true)
	protected List<PartyId> partyId;
	@XmlElement(name = "PartyRef", required = true)
	protected List<PartyRef> partyRef;
	@XmlElement(name = "CollaborationRole", required = true)
	protected List<CollaborationRole> collaborationRole;
	@XmlElement(name = "Certificate")
	protected List<Certificate> certificate;
	@XmlElement(name = "SecurityDetails")
	protected List<SecurityDetails> securityDetails;
	@XmlElement(name = "DeliveryChannel", required = true)
	protected List<DeliveryChannel> deliveryChannel;
	@XmlElement(name = "Transport", required = true)
	protected List<Transport> transport;
	@XmlElement(name = "DocExchange", required = true)
	protected List<DocExchange> docExchange;
	@XmlElement(name = "OverrideMshActionBinding")
	protected List<OverrideMshActionBinding> overrideMshActionBinding;
	@XmlAttribute(name = "partyName", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	protected String partyName;
	@XmlAttribute(name = "defaultMshChannelId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlIDREF
	@XmlSchemaType(name = "IDREF")
	protected Object defaultMshChannelId;
	@XmlAttribute(name = "defaultMshPackageId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlIDREF
	@XmlSchemaType(name = "IDREF")
	protected Object defaultMshPackageId;

	/**
	 * Gets the value of the partyId property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the partyId property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getPartyId().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link PartyId }
	 */
	public List<PartyId> getPartyId()
	{
		if (partyId == null)
		{
			partyId = new ArrayList<PartyId>();
		}
		return this.partyId;
	}

	/**
	 * Gets the value of the partyRef property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the partyRef property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getPartyRef().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link PartyRef }
	 */
	public List<PartyRef> getPartyRef()
	{
		if (partyRef == null)
		{
			partyRef = new ArrayList<PartyRef>();
		}
		return this.partyRef;
	}

	/**
	 * Gets the value of the collaborationRole property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the collaborationRole property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getCollaborationRole().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link CollaborationRole }
	 */
	public List<CollaborationRole> getCollaborationRole()
	{
		if (collaborationRole == null)
		{
			collaborationRole = new ArrayList<CollaborationRole>();
		}
		return this.collaborationRole;
	}

	/**
	 * Gets the value of the certificate property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the certificate property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getCertificate().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Certificate }
	 */
	public List<Certificate> getCertificate()
	{
		if (certificate == null)
		{
			certificate = new ArrayList<Certificate>();
		}
		return this.certificate;
	}

	/**
	 * Gets the value of the securityDetails property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the securityDetails property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getSecurityDetails().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link SecurityDetails }
	 */
	public List<SecurityDetails> getSecurityDetails()
	{
		if (securityDetails == null)
		{
			securityDetails = new ArrayList<SecurityDetails>();
		}
		return this.securityDetails;
	}

	/**
	 * Gets the value of the deliveryChannel property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the deliveryChannel property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getDeliveryChannel().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link DeliveryChannel }
	 */
	public List<DeliveryChannel> getDeliveryChannel()
	{
		if (deliveryChannel == null)
		{
			deliveryChannel = new ArrayList<DeliveryChannel>();
		}
		return this.deliveryChannel;
	}

	/**
	 * Gets the value of the transport property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the transport property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getTransport().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Transport }
	 */
	public List<Transport> getTransport()
	{
		if (transport == null)
		{
			transport = new ArrayList<Transport>();
		}
		return this.transport;
	}

	/**
	 * Gets the value of the docExchange property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the docExchange property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getDocExchange().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link DocExchange }
	 */
	public List<DocExchange> getDocExchange()
	{
		if (docExchange == null)
		{
			docExchange = new ArrayList<DocExchange>();
		}
		return this.docExchange;
	}

	/**
	 * Gets the value of the overrideMshActionBinding property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the overrideMshActionBinding property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getOverrideMshActionBinding().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link OverrideMshActionBinding }
	 */
	public List<OverrideMshActionBinding> getOverrideMshActionBinding()
	{
		if (overrideMshActionBinding == null)
		{
			overrideMshActionBinding = new ArrayList<OverrideMshActionBinding>();
		}
		return this.overrideMshActionBinding;
	}

	/**
	 * Gets the value of the partyName property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getPartyName()
	{
		return partyName;
	}

	/**
	 * Sets the value of the partyName property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setPartyName(String value)
	{
		this.partyName = value;
	}

	/**
	 * Gets the value of the defaultMshChannelId property.
	 * 
	 * @return possible object is {@link Object }
	 */
	public Object getDefaultMshChannelId()
	{
		return defaultMshChannelId;
	}

	/**
	 * Sets the value of the defaultMshChannelId property.
	 * 
	 * @param value allowed object is {@link Object }
	 */
	public void setDefaultMshChannelId(Object value)
	{
		this.defaultMshChannelId = value;
	}

	/**
	 * Gets the value of the defaultMshPackageId property.
	 * 
	 * @return possible object is {@link Object }
	 */
	public Object getDefaultMshPackageId()
	{
		return defaultMshPackageId;
	}

	/**
	 * Sets the value of the defaultMshPackageId property.
	 * 
	 * @param value allowed object is {@link Object }
	 */
	public void setDefaultMshPackageId(Object value)
	{
		this.defaultMshPackageId = value;
	}

}
