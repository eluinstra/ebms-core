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
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;

/**
 * <p>
 * Java class for anonymous complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd>non-empty-string">
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang use="required""/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"value"})
@XmlRootElement(name = "Description")
public class Description implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlValue
	protected String value;
	@XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlSchemaType(name = "language")
	protected String lang;

	/**
	 * Gets the value of the value property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getValue()
	{
		return value;
	}

	/**
	 * Sets the value of the value property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setValue(String value)
	{
		this.value = value;
	}

	/**
	 * Gets the value of the lang property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getLang()
	{
		return lang;
	}

	/**
	 * Sets the value of the lang property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setLang(String value)
	{
		this.lang = value;
	}

}
