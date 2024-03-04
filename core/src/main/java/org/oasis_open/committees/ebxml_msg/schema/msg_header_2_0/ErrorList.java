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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}Error" maxOccurs="unbounded"/>
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}headerExtension.grp"/>
 *       &lt;attribute name="highestSeverity" use="required" type="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}severity.type" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"error", "any"})
@XmlRootElement(name = "ErrorList")
public class ErrorList implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "Error", required = true)
	protected List<Error> error;
	@XmlAnyElement(lax = true)
	protected List<Object> any;
	@XmlAttribute(name = "highestSeverity", namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", required = true)
	protected SeverityType highestSeverity;
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
	 * Gets the value of the error property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the error property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getError().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Error }
	 */
	public List<Error> getError()
	{
		if (error == null)
		{
			error = new ArrayList<Error>();
		}
		return this.error;
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
	 * Gets the value of the highestSeverity property.
	 * 
	 * @return possible object is {@link SeverityType }
	 */
	public SeverityType getHighestSeverity()
	{
		return highestSeverity;
	}

	/**
	 * Sets the value of the highestSeverity property.
	 * 
	 * @param value allowed object is {@link SeverityType }
	 */
	public void setHighestSeverity(SeverityType value)
	{
		this.highestSeverity = value;
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
