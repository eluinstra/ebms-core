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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}MessagingCharacteristics"/>
 *       &lt;/sequence>
 *       &lt;attribute name="channelId" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="transportId" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="docExchangeId" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"messagingCharacteristics"})
@XmlRootElement(name = "DeliveryChannel")
public class DeliveryChannel implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "MessagingCharacteristics", required = true)
	protected MessagingCharacteristics messagingCharacteristics;
	@XmlAttribute(name = "channelId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String channelId;
	@XmlAttribute(name = "transportId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlIDREF
	@XmlSchemaType(name = "IDREF")
	protected Object transportId;
	@XmlAttribute(name = "docExchangeId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlIDREF
	@XmlSchemaType(name = "IDREF")
	protected Object docExchangeId;

	/**
	 * Gets the value of the messagingCharacteristics property.
	 * 
	 * @return possible object is {@link MessagingCharacteristics }
	 */
	public MessagingCharacteristics getMessagingCharacteristics()
	{
		return messagingCharacteristics;
	}

	/**
	 * Sets the value of the messagingCharacteristics property.
	 * 
	 * @param value allowed object is {@link MessagingCharacteristics }
	 */
	public void setMessagingCharacteristics(MessagingCharacteristics value)
	{
		this.messagingCharacteristics = value;
	}

	/**
	 * Gets the value of the channelId property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getChannelId()
	{
		return channelId;
	}

	/**
	 * Sets the value of the channelId property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setChannelId(String value)
	{
		this.channelId = value;
	}

	/**
	 * Gets the value of the transportId property.
	 * 
	 * @return possible object is {@link Object }
	 */
	public Object getTransportId()
	{
		return transportId;
	}

	/**
	 * Sets the value of the transportId property.
	 * 
	 * @param value allowed object is {@link Object }
	 */
	public void setTransportId(Object value)
	{
		this.transportId = value;
	}

	/**
	 * Gets the value of the docExchangeId property.
	 * 
	 * @return possible object is {@link Object }
	 */
	public Object getDocExchangeId()
	{
		return docExchangeId;
	}

	/**
	 * Sets the value of the docExchangeId property.
	 * 
	 * @param value allowed object is {@link Object }
	 */
	public void setDocExchangeId(Object value)
	{
		this.docExchangeId = value;
	}

}
