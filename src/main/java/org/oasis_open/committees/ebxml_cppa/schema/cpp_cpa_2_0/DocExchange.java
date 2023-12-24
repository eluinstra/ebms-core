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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ebXMLSenderBinding" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ebXMLReceiverBinding" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="docExchangeId" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"ebXMLSenderBinding", "ebXMLReceiverBinding"})
@XmlRootElement(name = "DocExchange")
public class DocExchange implements Serializable
{

	private final static long serialVersionUID = 1L;
	protected EbXMLSenderBinding ebXMLSenderBinding;
	protected EbXMLReceiverBinding ebXMLReceiverBinding;
	@XmlAttribute(name = "docExchangeId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String docExchangeId;

	/**
	 * Gets the value of the ebXMLSenderBinding property.
	 * 
	 * @return possible object is {@link EbXMLSenderBinding }
	 */
	public EbXMLSenderBinding getEbXMLSenderBinding()
	{
		return ebXMLSenderBinding;
	}

	/**
	 * Sets the value of the ebXMLSenderBinding property.
	 * 
	 * @param value allowed object is {@link EbXMLSenderBinding }
	 */
	public void setEbXMLSenderBinding(EbXMLSenderBinding value)
	{
		this.ebXMLSenderBinding = value;
	}

	/**
	 * Gets the value of the ebXMLReceiverBinding property.
	 * 
	 * @return possible object is {@link EbXMLReceiverBinding }
	 */
	public EbXMLReceiverBinding getEbXMLReceiverBinding()
	{
		return ebXMLReceiverBinding;
	}

	/**
	 * Sets the value of the ebXMLReceiverBinding property.
	 * 
	 * @param value allowed object is {@link EbXMLReceiverBinding }
	 */
	public void setEbXMLReceiverBinding(EbXMLReceiverBinding value)
	{
		this.ebXMLReceiverBinding = value;
	}

	/**
	 * Gets the value of the docExchangeId property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getDocExchangeId()
	{
		return docExchangeId;
	}

	/**
	 * Sets the value of the docExchangeId property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setDocExchangeId(String value)
	{
		this.docExchangeId = value;
	}

}
