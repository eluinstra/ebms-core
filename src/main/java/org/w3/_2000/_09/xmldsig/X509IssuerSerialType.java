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
package org.w3._2000._09.xmldsig;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * <p>
 * Java class for X509IssuerSerialType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="X509IssuerSerialType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="X509IssuerName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="X509SerialNumber" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "X509IssuerSerialType", propOrder = {"x509IssuerName", "x509SerialNumber"})
public class X509IssuerSerialType implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "X509IssuerName", required = true)
	protected String x509IssuerName;
	@XmlElement(name = "X509SerialNumber", required = true)
	protected BigInteger x509SerialNumber;

	/**
	 * Gets the value of the x509IssuerName property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getX509IssuerName()
	{
		return x509IssuerName;
	}

	/**
	 * Sets the value of the x509IssuerName property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setX509IssuerName(String value)
	{
		this.x509IssuerName = value;
	}

	/**
	 * Gets the value of the x509SerialNumber property.
	 * 
	 * @return possible object is {@link BigInteger }
	 */
	public BigInteger getX509SerialNumber()
	{
		return x509SerialNumber;
	}

	/**
	 * Sets the value of the x509SerialNumber property.
	 * 
	 * @param value allowed object is {@link BigInteger }
	 */
	public void setX509SerialNumber(BigInteger value)
	{
		this.x509SerialNumber = value;
	}

}
