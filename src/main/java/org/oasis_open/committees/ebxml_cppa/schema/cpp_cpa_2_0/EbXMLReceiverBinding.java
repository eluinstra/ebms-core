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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import nl.clockwork.ebms.jaxb.DurationAdapter;

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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ReliableMessaging" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}PersistDuration" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ReceiverNonRepudiation" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ReceiverDigitalEnvelope" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}NamespaceSupported" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}version use="required""/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"reliableMessaging", "persistDuration", "receiverNonRepudiation", "receiverDigitalEnvelope", "namespaceSupported"})
@XmlRootElement(name = "ebXMLReceiverBinding")
public class EbXMLReceiverBinding implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "ReliableMessaging")
	protected ReliableMessaging reliableMessaging;
	@XmlElement(name = "PersistDuration", type = String.class)
	@XmlJavaTypeAdapter(DurationAdapter.class)
	@XmlSchemaType(name = "duration")
	protected Duration persistDuration;
	@XmlElement(name = "ReceiverNonRepudiation")
	protected ReceiverNonRepudiation receiverNonRepudiation;
	@XmlElement(name = "ReceiverDigitalEnvelope")
	protected ReceiverDigitalEnvelope receiverDigitalEnvelope;
	@XmlElement(name = "NamespaceSupported")
	protected List<NamespaceSupported> namespaceSupported;
	@XmlAttribute(name = "version", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	protected String version;

	/**
	 * Gets the value of the reliableMessaging property.
	 * 
	 * @return possible object is {@link ReliableMessaging }
	 */
	public ReliableMessaging getReliableMessaging()
	{
		return reliableMessaging;
	}

	/**
	 * Sets the value of the reliableMessaging property.
	 * 
	 * @param value allowed object is {@link ReliableMessaging }
	 */
	public void setReliableMessaging(ReliableMessaging value)
	{
		this.reliableMessaging = value;
	}

	/**
	 * Gets the value of the persistDuration property.
	 * 
	 * @return possible object is {@link String }
	 */
	public Duration getPersistDuration()
	{
		return persistDuration;
	}

	/**
	 * Sets the value of the persistDuration property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setPersistDuration(Duration value)
	{
		this.persistDuration = value;
	}

	/**
	 * Gets the value of the receiverNonRepudiation property.
	 * 
	 * @return possible object is {@link ReceiverNonRepudiation }
	 */
	public ReceiverNonRepudiation getReceiverNonRepudiation()
	{
		return receiverNonRepudiation;
	}

	/**
	 * Sets the value of the receiverNonRepudiation property.
	 * 
	 * @param value allowed object is {@link ReceiverNonRepudiation }
	 */
	public void setReceiverNonRepudiation(ReceiverNonRepudiation value)
	{
		this.receiverNonRepudiation = value;
	}

	/**
	 * Gets the value of the receiverDigitalEnvelope property.
	 * 
	 * @return possible object is {@link ReceiverDigitalEnvelope }
	 */
	public ReceiverDigitalEnvelope getReceiverDigitalEnvelope()
	{
		return receiverDigitalEnvelope;
	}

	/**
	 * Sets the value of the receiverDigitalEnvelope property.
	 * 
	 * @param value allowed object is {@link ReceiverDigitalEnvelope }
	 */
	public void setReceiverDigitalEnvelope(ReceiverDigitalEnvelope value)
	{
		this.receiverDigitalEnvelope = value;
	}

	/**
	 * Gets the value of the namespaceSupported property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the namespaceSupported property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getNamespaceSupported().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link NamespaceSupported }
	 */
	public List<NamespaceSupported> getNamespaceSupported()
	{
		if (namespaceSupported == null)
		{
			namespaceSupported = new ArrayList<NamespaceSupported>();
		}
		return this.namespaceSupported;
	}

	/**
	 * Gets the value of the version property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * Sets the value of the version property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setVersion(String value)
	{
		this.version = value;
	}

}
