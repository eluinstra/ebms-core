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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Status"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Start"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}End"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ConversationConstraints" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}PartyInfo" maxOccurs="2" minOccurs="2"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}SimplePart" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Packaging" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Signature" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Comment" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="cpaid" use="required" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}non-empty-string" />
 *       &lt;attribute ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}version use="required""/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"status", "start", "end", "conversationConstraints", "partyInfo", "simplePart", "packaging", "signature", "comment"})
@XmlRootElement(name = "CollaborationProtocolAgreement")
public class CollaborationProtocolAgreement implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "Status", required = true)
	protected Status status;
	@XmlElement(name = "Start", required = true, type = String.class)
	@XmlJavaTypeAdapter(InstantAdapter.class)
	@XmlSchemaType(name = "dateTime")
	protected Instant start;
	@XmlElement(name = "End", required = true, type = String.class)
	@XmlJavaTypeAdapter(InstantAdapter.class)
	@XmlSchemaType(name = "dateTime")
	protected Instant end;
	@XmlElement(name = "ConversationConstraints")
	protected ConversationConstraints conversationConstraints;
	@XmlElement(name = "PartyInfo", required = true)
	protected List<PartyInfo> partyInfo;
	@XmlElement(name = "SimplePart", required = true)
	protected List<SimplePart> simplePart;
	@XmlElement(name = "Packaging", required = true)
	protected List<Packaging> packaging;
	@XmlElement(name = "Signature")
	protected Signature signature;
	@XmlElement(name = "Comment")
	protected List<Comment> comment;
	@XmlAttribute(name = "cpaid", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	protected String cpaid;
	@XmlAttribute(name = "version", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	protected String version;

	/**
	 * Gets the value of the status property.
	 * 
	 * @return possible object is {@link Status }
	 */
	public Status getStatus()
	{
		return status;
	}

	/**
	 * Sets the value of the status property.
	 * 
	 * @param value allowed object is {@link Status }
	 */
	public void setStatus(Status value)
	{
		this.status = value;
	}

	/**
	 * Gets the value of the start property.
	 * 
	 * @return possible object is {@link String }
	 */
	public Instant getStart()
	{
		return start;
	}

	/**
	 * Sets the value of the start property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setStart(Instant value)
	{
		this.start = value;
	}

	/**
	 * Gets the value of the end property.
	 * 
	 * @return possible object is {@link String }
	 */
	public Instant getEnd()
	{
		return end;
	}

	/**
	 * Sets the value of the end property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setEnd(Instant value)
	{
		this.end = value;
	}

	/**
	 * Gets the value of the conversationConstraints property.
	 * 
	 * @return possible object is {@link ConversationConstraints }
	 */
	public ConversationConstraints getConversationConstraints()
	{
		return conversationConstraints;
	}

	/**
	 * Sets the value of the conversationConstraints property.
	 * 
	 * @param value allowed object is {@link ConversationConstraints }
	 */
	public void setConversationConstraints(ConversationConstraints value)
	{
		this.conversationConstraints = value;
	}

	/**
	 * Gets the value of the partyInfo property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the partyInfo property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getPartyInfo().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link PartyInfo }
	 */
	public List<PartyInfo> getPartyInfo()
	{
		if (partyInfo == null)
		{
			partyInfo = new ArrayList<PartyInfo>();
		}
		return this.partyInfo;
	}

	/**
	 * Gets the value of the simplePart property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the simplePart property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getSimplePart().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link SimplePart }
	 */
	public List<SimplePart> getSimplePart()
	{
		if (simplePart == null)
		{
			simplePart = new ArrayList<SimplePart>();
		}
		return this.simplePart;
	}

	/**
	 * Gets the value of the packaging property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the packaging property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getPackaging().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Packaging }
	 */
	public List<Packaging> getPackaging()
	{
		if (packaging == null)
		{
			packaging = new ArrayList<Packaging>();
		}
		return this.packaging;
	}

	/**
	 * Gets the value of the signature property.
	 * 
	 * @return possible object is {@link Signature }
	 */
	public Signature getSignature()
	{
		return signature;
	}

	/**
	 * Sets the value of the signature property.
	 * 
	 * @param value allowed object is {@link Signature }
	 */
	public void setSignature(Signature value)
	{
		this.signature = value;
	}

	/**
	 * Gets the value of the comment property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the comment property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getComment().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Comment }
	 */
	public List<Comment> getComment()
	{
		if (comment == null)
		{
			comment = new ArrayList<Comment>();
		}
		return this.comment;
	}

	/**
	 * Gets the value of the cpaid property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getCpaid()
	{
		return cpaid;
	}

	/**
	 * Sets the value of the cpaid property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setCpaid(String value)
	{
		this.cpaid = value;
	}

	/**
	 * Gets the value of the version property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * Sets the value of the version property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setVersion(String value)
	{
		this.version = value;
	}

}
