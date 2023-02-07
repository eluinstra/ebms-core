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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}TrustAnchors" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}SecurityPolicy" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="securityId" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"trustAnchors", "securityPolicy"})
@XmlRootElement(name = "SecurityDetails")
public class SecurityDetails implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "TrustAnchors")
	protected TrustAnchors trustAnchors;
	@XmlElement(name = "SecurityPolicy")
	protected SecurityPolicy securityPolicy;
	@XmlAttribute(name = "securityId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String securityId;

	/**
	 * Gets the value of the trustAnchors property.
	 * 
	 * @return possible object is {@link TrustAnchors }
	 */
	public TrustAnchors getTrustAnchors()
	{
		return trustAnchors;
	}

	/**
	 * Sets the value of the trustAnchors property.
	 * 
	 * @param value allowed object is {@link TrustAnchors }
	 */
	public void setTrustAnchors(TrustAnchors value)
	{
		this.trustAnchors = value;
	}

	/**
	 * Gets the value of the securityPolicy property.
	 * 
	 * @return possible object is {@link SecurityPolicy }
	 */
	public SecurityPolicy getSecurityPolicy()
	{
		return securityPolicy;
	}

	/**
	 * Sets the value of the securityPolicy property.
	 * 
	 * @param value allowed object is {@link SecurityPolicy }
	 */
	public void setSecurityPolicy(SecurityPolicy value)
	{
		this.securityPolicy = value;
	}

	/**
	 * Gets the value of the securityId property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getSecurityId()
	{
		return securityId;
	}

	/**
	 * Sets the value of the securityId property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setSecurityId(String value)
	{
		this.securityId = value;
	}

}
