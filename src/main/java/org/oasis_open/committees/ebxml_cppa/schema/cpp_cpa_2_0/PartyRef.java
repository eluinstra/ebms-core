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
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}xlink.grp"/>
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="schemaLocation" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "PartyRef")
public class PartyRef implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlAttribute(name = "type", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	@XmlSchemaType(name = "anyURI")
	protected String type;
	@XmlAttribute(name = "schemaLocation", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	@XmlSchemaType(name = "anyURI")
	protected String schemaLocation;
	@XmlAttribute(name = "type", namespace = "http://www.w3.org/1999/xlink")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String xLinkType;
	@XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink", required = true)
	@XmlSchemaType(name = "anyURI")
	protected String xLinkHRef;

	/**
	 * Gets the value of the type property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * Sets the value of the type property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setType(String value)
	{
		this.type = value;
	}

	/**
	 * Gets the value of the schemaLocation property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getSchemaLocation()
	{
		return schemaLocation;
	}

	/**
	 * Sets the value of the schemaLocation property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setSchemaLocation(String value)
	{
		this.schemaLocation = value;
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
