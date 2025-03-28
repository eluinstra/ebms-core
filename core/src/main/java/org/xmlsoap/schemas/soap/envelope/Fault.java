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
package org.xmlsoap.schemas.soap.envelope;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import javax.xml.namespace.QName;

/**
 * Fault reporting structure
 * <p>
 * Java class for Fault complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Fault">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="faultcode" type="{http://www.w3.org/2001/XMLSchema}QName"/>
 *         &lt;element name="faultstring" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="faultactor" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 *         &lt;element name="detail" type="{http://schemas.xmlsoap.org/soap/envelope/}detail" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Fault", propOrder = {"faultcode", "faultstring", "faultactor", "detail"})
public class Fault implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(required = true)
	protected QName faultcode;
	@XmlElement(required = true)
	protected String faultstring;
	@XmlSchemaType(name = "anyURI")
	protected String faultactor;
	protected Detail detail;

	/**
	 * Gets the value of the faultcode property.
	 * 
	 * @return possible object is {@link QName }
	 */
	public QName getFaultcode()
	{
		return faultcode;
	}

	/**
	 * Sets the value of the faultcode property.
	 * 
	 * @param value allowed object is {@link QName }
	 */
	public void setFaultcode(QName value)
	{
		this.faultcode = value;
	}

	/**
	 * Gets the value of the faultstring property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getFaultstring()
	{
		return faultstring;
	}

	/**
	 * Sets the value of the faultstring property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setFaultstring(String value)
	{
		this.faultstring = value;
	}

	/**
	 * Gets the value of the faultactor property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getFaultactor()
	{
		return faultactor;
	}

	/**
	 * Sets the value of the faultactor property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setFaultactor(String value)
	{
		this.faultactor = value;
	}

	/**
	 * Gets the value of the detail property.
	 * 
	 * @return possible object is {@link Detail }
	 */
	public Detail getDetail()
	{
		return detail;
	}

	/**
	 * Sets the value of the detail property.
	 * 
	 * @param value allowed object is {@link Detail }
	 */
	public void setDetail(Detail value)
	{
		this.detail = value;
	}

}
