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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.w3._2000._09.xmldsig.ReferenceType;

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
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}Reference" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}xlink.grp"/>
 *       &lt;attribute name="name" use="required" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}non-empty-string" />
 *       &lt;attribute ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}version use="required""/>
 *       &lt;attribute name="uuid" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"reference"})
@XmlRootElement(name = "ProcessSpecification")
public class ProcessSpecification implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "Reference", namespace = "http://www.w3.org/2000/09/xmldsig#")
	protected List<ReferenceType> reference;
	@XmlAttribute(name = "name", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	protected String name;
	@XmlAttribute(name = "version", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	protected String version;
	@XmlAttribute(name = "uuid", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	@XmlSchemaType(name = "anyURI")
	protected String uuid;
	@XmlAttribute(name = "type", namespace = "http://www.w3.org/1999/xlink")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String xLinkType;
	@XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink", required = true)
	@XmlSchemaType(name = "anyURI")
	protected String xLinkHRef;

	/**
	 * Gets the value of the reference property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the reference property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getReference().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link ReferenceType }
	 */
	public List<ReferenceType> getReference()
	{
		if (reference == null)
		{
			reference = new ArrayList<ReferenceType>();
		}
		return this.reference;
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the value of the name property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setName(String value)
	{
		this.name = value;
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
	 * Gets the value of the uuid property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * Sets the value of the uuid property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setUuid(String value)
	{
		this.uuid = value;
	}

	/**
	 * Gets the value of the xLinkType property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getXLinkType()
	{
		if (xLinkType == null)
		{
			return "simple";
		}
		else
		{
			return xLinkType;
		}
	}

	/**
	 * Sets the value of the xLinkType property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setXLinkType(String value)
	{
		this.xLinkType = value;
	}

	/**
	 * Gets the value of the xLinkHRef property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getXLinkHRef()
	{
		return xLinkHRef;
	}

	/**
	 * Sets the value of the xLinkHRef property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setXLinkHRef(String value)
	{
		this.xLinkHRef = value;
	}

}
