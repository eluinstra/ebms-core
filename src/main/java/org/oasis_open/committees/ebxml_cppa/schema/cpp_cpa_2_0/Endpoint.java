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
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;

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
 *       &lt;attribute name="uri" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="type" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}endpointType.type" default="allPurpose" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "Endpoint")
public class Endpoint implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlAttribute(name = "uri", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlSchemaType(name = "anyURI")
	protected String uri;
	@XmlAttribute(name = "type", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected EndpointTypeType type;

	/**
	 * Gets the value of the uri property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getUri()
	{
		return uri;
	}

	/**
	 * Sets the value of the uri property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setUri(String value)
	{
		this.uri = value;
	}

	/**
	 * Gets the value of the type property.
	 * 
	 * @return possible object is {@link EndpointTypeType }
	 */
	public EndpointTypeType getType()
	{
		if (type == null)
		{
			return EndpointTypeType.ALL_PURPOSE;
		}
		else
		{
			return type;
		}
	}

	/**
	 * Sets the value of the type property.
	 * 
	 * @param value allowed object is {@link EndpointTypeType }
	 */
	public void setType(EndpointTypeType value)
	{
		this.type = value;
	}

}
