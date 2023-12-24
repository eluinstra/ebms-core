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
import java.math.BigInteger;
import java.time.Duration;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import nl.clockwork.ebms.jaxb.DurationAdapter;

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
 *         &lt;element name="Retries" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="RetryInterval" type="{http://www.w3.org/2001/XMLSchema}duration" minOccurs="0"/>
 *         &lt;element name="MessageOrderSemantics" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}messageOrderSemantics.type"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"retries", "retryInterval", "messageOrderSemantics"})
@XmlRootElement(name = "ReliableMessaging")
public class ReliableMessaging implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "Retries")
	protected BigInteger retries;
	@XmlElement(name = "RetryInterval", type = String.class)
	@XmlJavaTypeAdapter(DurationAdapter.class)
	@XmlSchemaType(name = "duration")
	protected Duration retryInterval;
	@XmlElement(name = "MessageOrderSemantics", required = true)
	protected MessageOrderSemanticsType messageOrderSemantics;

	/**
	 * Gets the value of the retries property.
	 * 
	 * @return possible object is {@link BigInteger }
	 */
	public BigInteger getRetries()
	{
		return retries;
	}

	/**
	 * Sets the value of the retries property.
	 * 
	 * @param value allowed object is {@link BigInteger }
	 */
	public void setRetries(BigInteger value)
	{
		this.retries = value;
	}

	/**
	 * Gets the value of the retryInterval property.
	 * 
	 * @return possible object is {@link String }
	 */
	public Duration getRetryInterval()
	{
		return retryInterval;
	}

	/**
	 * Sets the value of the retryInterval property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setRetryInterval(Duration value)
	{
		this.retryInterval = value;
	}

	/**
	 * Gets the value of the messageOrderSemantics property.
	 * 
	 * @return possible object is {@link MessageOrderSemanticsType }
	 */
	public MessageOrderSemanticsType getMessageOrderSemantics()
	{
		return messageOrderSemantics;
	}

	/**
	 * Sets the value of the messageOrderSemantics property.
	 * 
	 * @param value allowed object is {@link MessageOrderSemanticsType }
	 */
	public void setMessageOrderSemantics(MessageOrderSemanticsType value)
	{
		this.messageOrderSemantics = value;
	}

}
