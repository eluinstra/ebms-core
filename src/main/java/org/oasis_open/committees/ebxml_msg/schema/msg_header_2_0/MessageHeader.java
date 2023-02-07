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
package org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.runtime.ZeroOneBooleanAdapter;
import org.w3c.dom.Element;

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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}From"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}To"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}CPAId"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}ConversationId"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}Service"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}Action"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}MessageData"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}DuplicateElimination" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}Description" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}headerExtension.grp"/>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"from", "to", "cpaId", "conversationId", "service", "action", "messageData", "duplicateElimination", "description", "any"})
@XmlRootElement(name = "MessageHeader")
public class MessageHeader implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "From", required = true)
	protected From from;
	@XmlElement(name = "To", required = true)
	protected To to;
	@XmlElement(name = "CPAId", required = true)
	protected String cpaId;
	@XmlElement(name = "ConversationId", required = true)
	protected String conversationId;
	@XmlElement(name = "Service", required = true)
	protected Service service;
	@XmlElement(name = "Action", required = true)
	protected String action;
	@XmlElement(name = "MessageData", required = true)
	protected MessageData messageData;
	@XmlElement(name = "DuplicateElimination")
	protected String duplicateElimination;
	@XmlElement(name = "Description")
	protected List<Description> description;
	@XmlAnyElement(lax = true)
	protected List<Object> any;
	@XmlAttribute(name = "id", namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String id;
	@XmlAttribute(name = "version", namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", required = true)
	protected String version;
	@XmlAttribute(name = "mustUnderstand", namespace = "http://schemas.xmlsoap.org/soap/envelope/", required = true)
	@XmlJavaTypeAdapter(ZeroOneBooleanAdapter.class)
	protected Boolean mustUnderstand;
	@XmlAnyAttribute
	private Map<QName, String> otherAttributes = new HashMap<QName, String>();

	/**
	 * Gets the value of the from property.
	 * 
	 * @return possible object is {@link From }
	 */
	public From getFrom()
	{
		return from;
	}

	/**
	 * Sets the value of the from property.
	 * 
	 * @param value allowed object is {@link From }
	 */
	public void setFrom(From value)
	{
		this.from = value;
	}

	/**
	 * Gets the value of the to property.
	 * 
	 * @return possible object is {@link To }
	 */
	public To getTo()
	{
		return to;
	}

	/**
	 * Sets the value of the to property.
	 * 
	 * @param value allowed object is {@link To }
	 */
	public void setTo(To value)
	{
		this.to = value;
	}

	/**
	 * Gets the value of the cpaId property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getCPAId()
	{
		return cpaId;
	}

	/**
	 * Sets the value of the cpaId property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setCPAId(String value)
	{
		this.cpaId = value;
	}

	/**
	 * Gets the value of the conversationId property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getConversationId()
	{
		return conversationId;
	}

	/**
	 * Sets the value of the conversationId property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setConversationId(String value)
	{
		this.conversationId = value;
	}

	/**
	 * Gets the value of the service property.
	 * 
	 * @return possible object is {@link Service }
	 */
	public Service getService()
	{
		return service;
	}

	/**
	 * Sets the value of the service property.
	 * 
	 * @param value allowed object is {@link Service }
	 */
	public void setService(Service value)
	{
		this.service = value;
	}

	/**
	 * Gets the value of the action property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getAction()
	{
		return action;
	}

	/**
	 * Sets the value of the action property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setAction(String value)
	{
		this.action = value;
	}

	/**
	 * Gets the value of the messageData property.
	 * 
	 * @return possible object is {@link MessageData }
	 */
	public MessageData getMessageData()
	{
		return messageData;
	}

	/**
	 * Sets the value of the messageData property.
	 * 
	 * @param value allowed object is {@link MessageData }
	 */
	public void setMessageData(MessageData value)
	{
		this.messageData = value;
	}

	/**
	 * Gets the value of the duplicateElimination property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getDuplicateElimination()
	{
		return duplicateElimination;
	}

	/**
	 * Sets the value of the duplicateElimination property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setDuplicateElimination(String value)
	{
		this.duplicateElimination = value;
	}

	/**
	 * Gets the value of the description property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the description property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getDescription().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Description }
	 */
	public List<Description> getDescription()
	{
		if (description == null)
		{
			description = new ArrayList<Description>();
		}
		return this.description;
	}

	/**
	 * Gets the value of the any property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the any property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getAny().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Element } {@link Object }
	 */
	public List<Object> getAny()
	{
		if (any == null)
		{
			any = new ArrayList<Object>();
		}
		return this.any;
	}

	/**
	 * Gets the value of the id property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Sets the value of the id property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setId(String value)
	{
		this.id = value;
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

	/**
	 * Gets the value of the mustUnderstand property.
	 * 
	 * @return possible object is {@link String }
	 */
	public Boolean isMustUnderstand()
	{
		return mustUnderstand;
	}

	/**
	 * Sets the value of the mustUnderstand property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setMustUnderstand(Boolean value)
	{
		this.mustUnderstand = value;
	}

	/**
	 * Gets a map that contains attributes that aren't bound to any typed property on this class.
	 * <p>
	 * the map is keyed by the name of the attribute and the value is the string value of the attribute. the map returned by this method is live, and you can add
	 * new attribute by updating the map directly. Because of this design, there's no setter.
	 * 
	 * @return always non-null
	 */
	public Map<QName, String> getOtherAttributes()
	{
		return otherAttributes;
	}

}
