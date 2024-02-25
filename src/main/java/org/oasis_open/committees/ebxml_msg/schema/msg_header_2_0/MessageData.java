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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.time.Instant;
import nl.clockwork.ebms.jaxb.InstantAdapter;

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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}MessageId"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}Timestamp"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}RefToMessageId" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}TimeToLive" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"messageId", "timestamp", "refToMessageId", "timeToLive"})
@XmlRootElement(name = "MessageData")
public class MessageData implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "MessageId", required = true)
	protected String messageId;
	@XmlElement(name = "Timestamp", required = true, type = String.class)
	@XmlJavaTypeAdapter(InstantAdapter.class)
	@XmlSchemaType(name = "dateTime")
	protected Instant timestamp;
	@XmlElement(name = "RefToMessageId")
	protected String refToMessageId;
	@XmlElement(name = "TimeToLive", type = String.class)
	@XmlJavaTypeAdapter(InstantAdapter.class)
	@XmlSchemaType(name = "dateTime")
	protected Instant timeToLive;

	/**
	 * Gets the value of the messageId property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getMessageId()
	{
		return messageId;
	}

	/**
	 * Sets the value of the messageId property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setMessageId(String value)
	{
		this.messageId = value;
	}

	/**
	 * Gets the value of the timestamp property.
	 * 
	 * @return possible object is {@link String }
	 */
	public Instant getTimestamp()
	{
		return timestamp;
	}

	/**
	 * Sets the value of the timestamp property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setTimestamp(Instant value)
	{
		this.timestamp = value;
	}

	/**
	 * Gets the value of the refToMessageId property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getRefToMessageId()
	{
		return refToMessageId;
	}

	/**
	 * Sets the value of the refToMessageId property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setRefToMessageId(String value)
	{
		this.refToMessageId = value;
	}

	/**
	 * Gets the value of the timeToLive property.
	 * 
	 * @return possible object is {@link String }
	 */
	public Instant getTimeToLive()
	{
		return timeToLive;
	}

	/**
	 * Sets the value of the timeToLive property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setTimeToLive(Instant value)
	{
		this.timeToLive = value;
	}

}
