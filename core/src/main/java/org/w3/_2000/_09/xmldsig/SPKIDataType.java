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
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

/**
 * <p>
 * Java class for SPKIDataType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SPKIDataType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence maxOccurs="unbounded">
 *         &lt;element name="SPKISexp" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/>
 *         &lt;any processContents='lax' namespace='##other' minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SPKIDataType", propOrder = {"spkiSexpAndAny"})
public class SPKIDataType implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElementRef(name = "SPKISexp", namespace = "http://www.w3.org/2000/09/xmldsig#", type = JAXBElement.class)
	@XmlAnyElement(lax = true)
	protected List<Object> spkiSexpAndAny;

	/**
	 * Gets the value of the spkiSexpAndAny property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the spkiSexpAndAny property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getSPKISexpAndAny().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Element } {@link JAXBElement }{@code <}{@link byte[]}{@code >} {@link Object }
	 */
	public List<Object> getSPKISexpAndAny()
	{
		if (spkiSexpAndAny == null)
		{
			spkiSexpAndAny = new ArrayList<Object>();
		}
		return this.spkiSexpAndAny;
	}

}
