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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}NamespaceSupported" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}pkg.grp"/>
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}role"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"namespaceSupported"})
@XmlRootElement(name = "SimplePart")
public class SimplePart implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "NamespaceSupported")
	protected List<NamespaceSupported> namespaceSupported;
	@XmlAttribute(name = "role", namespace = "http://www.w3.org/1999/xlink")
	@XmlSchemaType(name = "anyURI")
	protected String role;
	@XmlAttribute(name = "id", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String id;
	@XmlAttribute(name = "mimetype", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	protected String mimetype;
	@XmlAttribute(name = "mimeparameters", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected String mimeparameters;

	/**
	 * Gets the value of the namespaceSupported property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the namespaceSupported property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getNamespaceSupported().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link NamespaceSupported }
	 */
	public List<NamespaceSupported> getNamespaceSupported()
	{
		if (namespaceSupported == null)
		{
			namespaceSupported = new ArrayList<NamespaceSupported>();
		}
		return this.namespaceSupported;
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
	 * Gets the value of the mimetype property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getMimetype()
	{
		return mimetype;
	}

	/**
	 * Sets the value of the mimetype property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setMimetype(String value)
	{
		this.mimetype = value;
	}

	/**
	 * Gets the value of the mimeparameters property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getMimeparameters()
	{
		return mimeparameters;
	}

	/**
	 * Sets the value of the mimeparameters property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setMimeparameters(String value)
	{
		this.mimeparameters = value;
	}

}
