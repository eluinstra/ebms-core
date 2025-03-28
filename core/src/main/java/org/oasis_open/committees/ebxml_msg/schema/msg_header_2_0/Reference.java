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
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;

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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}Schema" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}Description" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute ref="{http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd}id"/>
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}type fixed="simple""/>
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}href use="required""/>
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}role"/>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"schema", "description", "any"})
@XmlRootElement(name = "Reference")
public class Reference implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "Schema")
	protected List<Schema> schema;
	@XmlElement(name = "Description")
	protected List<Description> description;
	@XmlAnyElement(lax = true)
	protected List<Object> any;
	@XmlAttribute(name = "id", namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String id;
	@XmlAttribute(name = "type", namespace = "http://www.w3.org/1999/xlink")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String type;
	@XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink", required = true)
	@XmlSchemaType(name = "anyURI")
	protected String href;
	@XmlAttribute(name = "role", namespace = "http://www.w3.org/1999/xlink")
	@XmlSchemaType(name = "anyURI")
	protected String role;
	@XmlAnyAttribute
	private Map<QName, String> otherAttributes = new HashMap<QName, String>();

	/**
	 * Gets the value of the schema property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the schema property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getSchema().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Schema }
	 */
	public List<Schema> getSchema()
	{
		if (schema == null)
		{
			schema = new ArrayList<Schema>();
		}
		return this.schema;
	}

	/**
	 * Gets the value of the description property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the description property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getDescription().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Description }
	 */
	public List<Description> getDescription()
	{
		if (description == null)
		{
			description = new ArrayList<Description>();
		}
		return this.description;
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
	 * Gets the value of the role property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getRole()
	{
		return role;
	}

	/**
	 * Sets the value of the role property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setRole(String value)
	{
		this.role = value;
	}

	/**
	 * Gets a map that contains attributes that aren't bound to any typed property on this class.
	 * <p>
	 * the map is keyed by the name of the attribute and the value is the string value of the attribute. the map returned by this method is live, and you can add
	 * new attribute by updating the map directly. Because of this design, there's no setter.
	 * 
	 * @return always non-null
	 */
	public Map<QName, String> getOtherAttributes()
	{
		return otherAttributes;
	}

}
