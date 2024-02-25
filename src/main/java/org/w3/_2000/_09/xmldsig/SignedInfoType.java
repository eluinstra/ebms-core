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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Java class for SignedInfoType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SignedInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}CanonicalizationMethod"/>
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}SignatureMethod"/>
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}Reference" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignedInfoType", propOrder = {"canonicalizationMethod", "signatureMethod", "reference"})
public class SignedInfoType implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "CanonicalizationMethod", required = true)
	protected CanonicalizationMethodType canonicalizationMethod;
	@XmlElement(name = "SignatureMethod", required = true)
	protected SignatureMethodType signatureMethod;
	@XmlElement(name = "Reference", required = true)
	protected List<ReferenceType> reference;
	@XmlAttribute(name = "Id")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String id;

	/**
	 * Gets the value of the canonicalizationMethod property.
	 * 
	 * @return possible object is {@link CanonicalizationMethodType }
	 */
	public CanonicalizationMethodType getCanonicalizationMethod()
	{
		return canonicalizationMethod;
	}

	/**
	 * Sets the value of the canonicalizationMethod property.
	 * 
	 * @param value allowed object is {@link CanonicalizationMethodType }
	 */
	public void setCanonicalizationMethod(CanonicalizationMethodType value)
	{
		this.canonicalizationMethod = value;
	}

	/**
	 * Gets the value of the signatureMethod property.
	 * 
	 * @return possible object is {@link SignatureMethodType }
	 */
	public SignatureMethodType getSignatureMethod()
	{
		return signatureMethod;
	}

	/**
	 * Sets the value of the signatureMethod property.
	 * 
	 * @param value allowed object is {@link SignatureMethodType }
	 */
	public void setSignatureMethod(SignatureMethodType value)
	{
		this.signatureMethod = value;
	}

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

}
