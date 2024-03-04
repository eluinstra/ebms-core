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
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import org.w3._2000._09.xmldsig.KeyInfoType;

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
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}KeyInfo"/>
 *       &lt;/sequence>
 *       &lt;attribute name="certId" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"keyInfo"})
@XmlRootElement(name = "Certificate")
public class Certificate implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "KeyInfo", namespace = "http://www.w3.org/2000/09/xmldsig#", required = true)
	protected KeyInfoType keyInfo;
	@XmlAttribute(name = "certId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String certId;

	/**
	 * Gets the value of the keyInfo property.
	 * 
	 * @return possible object is {@link KeyInfoType }
	 */
	public KeyInfoType getKeyInfo()
	{
		return keyInfo;
	}

	/**
	 * Sets the value of the keyInfo property.
	 * 
	 * @param value allowed object is {@link KeyInfoType }
	 */
	public void setKeyInfo(KeyInfoType value)
	{
		this.keyInfo = value;
	}

	/**
	 * Gets the value of the certId property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getCertId()
	{
		return certId;
	}

	/**
	 * Sets the value of the certId property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setCertId(String value)
	{
		this.certId = value;
	}

}
