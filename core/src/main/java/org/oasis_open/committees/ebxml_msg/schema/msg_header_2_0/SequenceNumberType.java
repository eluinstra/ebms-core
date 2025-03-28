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
package org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * <p>
 * Java class for sequenceNumber.type complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="sequenceNumber.type">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>nonNegativeInteger">
 *       &lt;attribute name="status" type="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}status.type" default="Continue" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sequenceNumber.type", propOrder = {"value"})
public class SequenceNumberType implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlValue
	@XmlSchemaType(name = "nonNegativeInteger")
	protected BigInteger value;
	@XmlAttribute(name = "status", namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd")
	protected StatusType status;

	/**
	 * Gets the value of the value property.
	 * 
	 * @return possible object is {@link BigInteger }
	 */
	public BigInteger getValue()
	{
		return value;
	}

	/**
	 * Sets the value of the value property.
	 * 
	 * @param value allowed object is {@link BigInteger }
	 */
	public void setValue(BigInteger value)
	{
		this.value = value;
	}

	/**
	 * Gets the value of the status property.
	 * 
	 * @return possible object is {@link StatusType }
	 */
	public StatusType getStatus()
	{
		if (status == null)
		{
			return StatusType.CONTINUE;
		}
		else
		{
			return status;
		}
	}

	/**
	 * Sets the value of the status property.
	 * 
	 * @param value allowed object is {@link StatusType }
	 */
	public void setStatus(StatusType value)
	{
		this.status = value;
	}

}
