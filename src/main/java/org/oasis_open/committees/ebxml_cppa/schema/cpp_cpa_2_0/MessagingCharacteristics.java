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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
 *       &lt;attribute ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}syncReplyMode default="none""/>
 *       &lt;attribute name="ackRequested" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}perMessageCharacteristics.type" default="perMessage" />
 *       &lt;attribute name="ackSignatureRequested" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}perMessageCharacteristics.type" default="perMessage" />
 *       &lt;attribute name="duplicateElimination" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}perMessageCharacteristics.type" default="perMessage" />
 *       &lt;attribute name="actor" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}actor.type" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "MessagingCharacteristics")
public class MessagingCharacteristics implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlAttribute(name = "syncReplyMode", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected SyncReplyModeType syncReplyMode;
	@XmlAttribute(name = "ackRequested", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected PerMessageCharacteristicsType ackRequested;
	@XmlAttribute(name = "ackSignatureRequested", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected PerMessageCharacteristicsType ackSignatureRequested;
	@XmlAttribute(name = "duplicateElimination", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected PerMessageCharacteristicsType duplicateElimination;
	@XmlAttribute(name = "actor", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected ActorType actor;

	/**
	 * Gets the value of the syncReplyMode property.
	 * 
	 * @return possible object is {@link SyncReplyModeType }
	 */
	public SyncReplyModeType getSyncReplyMode()
	{
		if (syncReplyMode == null)
		{
			return SyncReplyModeType.NONE;
		}
		else
		{
			return syncReplyMode;
		}
	}

	/**
	 * Sets the value of the syncReplyMode property.
	 * 
	 * @param value allowed object is {@link SyncReplyModeType }
	 */
	public void setSyncReplyMode(SyncReplyModeType value)
	{
		this.syncReplyMode = value;
	}

	/**
	 * Gets the value of the ackRequested property.
	 * 
	 * @return possible object is {@link PerMessageCharacteristicsType }
	 */
	public PerMessageCharacteristicsType getAckRequested()
	{
		if (ackRequested == null)
		{
			return PerMessageCharacteristicsType.PER_MESSAGE;
		}
		else
		{
			return ackRequested;
		}
	}

	/**
	 * Sets the value of the ackRequested property.
	 * 
	 * @param value allowed object is {@link PerMessageCharacteristicsType }
	 */
	public void setAckRequested(PerMessageCharacteristicsType value)
	{
		this.ackRequested = value;
	}

	/**
	 * Gets the value of the ackSignatureRequested property.
	 * 
	 * @return possible object is {@link PerMessageCharacteristicsType }
	 */
	public PerMessageCharacteristicsType getAckSignatureRequested()
	{
		if (ackSignatureRequested == null)
		{
			return PerMessageCharacteristicsType.PER_MESSAGE;
		}
		else
		{
			return ackSignatureRequested;
		}
	}

	/**
	 * Sets the value of the ackSignatureRequested property.
	 * 
	 * @param value allowed object is {@link PerMessageCharacteristicsType }
	 */
	public void setAckSignatureRequested(PerMessageCharacteristicsType value)
	{
		this.ackSignatureRequested = value;
	}

	/**
	 * Gets the value of the duplicateElimination property.
	 * 
	 * @return possible object is {@link PerMessageCharacteristicsType }
	 */
	public PerMessageCharacteristicsType getDuplicateElimination()
	{
		if (duplicateElimination == null)
		{
			return PerMessageCharacteristicsType.PER_MESSAGE;
		}
		else
		{
			return duplicateElimination;
		}
	}

	/**
	 * Sets the value of the duplicateElimination property.
	 * 
	 * @param value allowed object is {@link PerMessageCharacteristicsType }
	 */
	public void setDuplicateElimination(PerMessageCharacteristicsType value)
	{
		this.duplicateElimination = value;
	}

	/**
	 * Gets the value of the actor property.
	 * 
	 * @return possible object is {@link ActorType }
	 */
	public ActorType getActor()
	{
		return actor;
	}

	/**
	 * Sets the value of the actor property.
	 * 
	 * @param value allowed object is {@link ActorType }
	 */
	public void setActor(ActorType value)
	{
		this.actor = value;
	}

}
