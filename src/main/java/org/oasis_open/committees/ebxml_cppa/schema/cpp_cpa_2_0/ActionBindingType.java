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
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.w3c.dom.Element;

/**
 * <p>
 * Java class for ActionBinding.type complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ActionBinding.type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}BusinessTransactionCharacteristics"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ActionContext" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ChannelId" maxOccurs="unbounded"/>
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="action" use="required" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}non-empty-string" />
 *       &lt;attribute name="packageId" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}href"/>
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}type fixed="simple""/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActionBinding.type", propOrder = {"businessTransactionCharacteristics", "actionContext", "channelId", "any"})
public class ActionBindingType implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "BusinessTransactionCharacteristics", required = true)
	protected BusinessTransactionCharacteristics businessTransactionCharacteristics;
	@XmlElement(name = "ActionContext")
	protected ActionContext actionContext;
	@XmlElementRef(name = "ChannelId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", type = JAXBElement.class)
	protected List<JAXBElement<Object>> channelId;
	@XmlAnyElement(lax = true)
	protected List<Object> any;
	@XmlAttribute(name = "id", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String id;
	@XmlAttribute(name = "action", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	protected String action;
	@XmlAttribute(name = "packageId", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlIDREF
	@XmlSchemaType(name = "IDREF")
	protected Object packageId;
	@XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink")
	@XmlSchemaType(name = "anyURI")
	protected String href;
	@XmlAttribute(name = "type", namespace = "http://www.w3.org/1999/xlink")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String type;

	/**
	 * Gets the value of the businessTransactionCharacteristics property.
	 * 
	 * @return possible object is {@link BusinessTransactionCharacteristics }
	 */
	public BusinessTransactionCharacteristics getBusinessTransactionCharacteristics()
	{
		return businessTransactionCharacteristics;
	}

	/**
	 * Sets the value of the businessTransactionCharacteristics property.
	 * 
	 * @param value allowed object is {@link BusinessTransactionCharacteristics }
	 */
	public void setBusinessTransactionCharacteristics(BusinessTransactionCharacteristics value)
	{
		this.businessTransactionCharacteristics = value;
	}

	/**
	 * Gets the value of the actionContext property.
	 * 
	 * @return possible object is {@link ActionContext }
	 */
	public ActionContext getActionContext()
	{
		return actionContext;
	}

	/**
	 * Sets the value of the actionContext property.
	 * 
	 * @param value allowed object is {@link ActionContext }
	 */
	public void setActionContext(ActionContext value)
	{
		this.actionContext = value;
	}

	/**
	 * Gets the value of the channelId property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the channelId property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getChannelId().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link JAXBElement }{@code <}{@link Object }{@code >}
	 */
	public List<JAXBElement<Object>> getChannelId()
	{
		if (channelId == null)
		{
			channelId = new ArrayList<JAXBElement<Object>>();
		}
		return this.channelId;
	}

	/**
	 * Gets the value of the any property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the any property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getAny().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Element } {@link Object }
	 */
	public List<Object> getAny()
	{
		if (any == null)
		{
			any = new ArrayList<Object>();
		}
		return this.any;
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

	/**
	 * Gets the value of the action property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getAction()
	{
		return action;
	}

	/**
	 * Sets the value of the action property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setAction(String value)
	{
		this.action = value;
	}

	/**
	 * Gets the value of the packageId property.
	 * 
	 * @return possible object is {@link Object }
	 */
	public Object getPackageId()
	{
		return packageId;
	}

	/**
	 * Sets the value of the packageId property.
	 * 
	 * @param value allowed object is {@link Object }
	 */
	public void setPackageId(Object value)
	{
		this.packageId = value;
	}

	/**
	 * Gets the value of the href property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getHref()
	{
		return href;
	}

	/**
	 * Sets the value of the href property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setHref(String value)
	{
		this.href = value;
	}

	/**
	 * Gets the value of the type property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getType()
	{
		if (type == null)
		{
			return "simple";
		}
		else
		{
			return type;
		}
	}

	/**
	 * Sets the value of the type property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setType(String value)
	{
		this.type = value;
	}

}
