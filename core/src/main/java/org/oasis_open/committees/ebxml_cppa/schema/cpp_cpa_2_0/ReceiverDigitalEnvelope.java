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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
 *         &lt;element name="DigitalEnvelopeProtocol" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}protocol.type"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}EncryptionAlgorithm" maxOccurs="unbounded"/>
 *         &lt;element name="EncryptionCertificateRef" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}CertificateRef.type"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"digitalEnvelopeProtocol", "encryptionAlgorithm", "encryptionCertificateRef"})
@XmlRootElement(name = "ReceiverDigitalEnvelope")
public class ReceiverDigitalEnvelope implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "DigitalEnvelopeProtocol", required = true)
	protected ProtocolType digitalEnvelopeProtocol;
	@XmlElement(name = "EncryptionAlgorithm", required = true)
	protected List<EncryptionAlgorithm> encryptionAlgorithm;
	@XmlElement(name = "EncryptionCertificateRef", required = true)
	protected CertificateRefType encryptionCertificateRef;

	/**
	 * Gets the value of the digitalEnvelopeProtocol property.
	 * 
	 * @return possible object is {@link ProtocolType }
	 */
	public ProtocolType getDigitalEnvelopeProtocol()
	{
		return digitalEnvelopeProtocol;
	}

	/**
	 * Sets the value of the digitalEnvelopeProtocol property.
	 * 
	 * @param value allowed object is {@link ProtocolType }
	 */
	public void setDigitalEnvelopeProtocol(ProtocolType value)
	{
		this.digitalEnvelopeProtocol = value;
	}

	/**
	 * Gets the value of the encryptionAlgorithm property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the encryptionAlgorithm property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getEncryptionAlgorithm().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link EncryptionAlgorithm }
	 */
	public List<EncryptionAlgorithm> getEncryptionAlgorithm()
	{
		if (encryptionAlgorithm == null)
		{
			encryptionAlgorithm = new ArrayList<EncryptionAlgorithm>();
		}
		return this.encryptionAlgorithm;
	}

	/**
	 * Gets the value of the encryptionCertificateRef property.
	 * 
	 * @return possible object is {@link CertificateRefType }
	 */
	public CertificateRefType getEncryptionCertificateRef()
	{
		return encryptionCertificateRef;
	}

	/**
	 * Sets the value of the encryptionCertificateRef property.
	 * 
	 * @param value allowed object is {@link CertificateRefType }
	 */
	public void setEncryptionCertificateRef(CertificateRefType value)
	{
		this.encryptionCertificateRef = value;
	}

}
