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
package org.w3._2000._09.xmldsig;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

/**
 * <p>
 * Java class for KeyValueType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="KeyValueType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}DSAKeyValue"/>
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}RSAKeyValue"/>
 *         &lt;any processContents='lax' namespace='##other'/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KeyValueType", propOrder = {"content"})
public class KeyValueType implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElementRefs({@XmlElementRef(name = "DSAKeyValue", namespace = "http://www.w3.org/2000/09/xmldsig#", type = JAXBElement.class),
			@XmlElementRef(name = "RSAKeyValue", namespace = "http://www.w3.org/2000/09/xmldsig#", type = JAXBElement.class)})
	@XmlMixed
	@XmlAnyElement(lax = true)
	protected List<Object> content;

	/**
	 * Gets the value of the content property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the content property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getContent().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Object } {@link String } {@link JAXBElement }{@code <}{@link DSAKeyValueType }{@code >}
	 * {@link JAXBElement }{@code <}{@link RSAKeyValueType }{@code >} {@link Element }
	 */
	public List<Object> getContent()
	{
		if (content == null)
		{
			content = new ArrayList<Object>();
		}
		return this.content;
	}

}
