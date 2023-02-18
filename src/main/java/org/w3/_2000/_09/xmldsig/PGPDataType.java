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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Element;

/**
 * <p>
 * Java class for PGPDataType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PGPDataType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;sequence>
 *           &lt;element name="PGPKeyID" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/>
 *           &lt;element name="PGPKeyPacket" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/>
 *           &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;/sequence>
 *         &lt;sequence>
 *           &lt;element name="PGPKeyPacket" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/>
 *           &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;/sequence>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PGPDataType", propOrder = {"content"})
public class PGPDataType implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElementRefs({@XmlElementRef(name = "PGPKeyID", namespace = "http://www.w3.org/2000/09/xmldsig#", type = JAXBElement.class),
			@XmlElementRef(name = "PGPKeyPacket", namespace = "http://www.w3.org/2000/09/xmldsig#", type = JAXBElement.class)})
	@XmlAnyElement(lax = true)
	protected List<Object> content;

	/**
	 * Gets the rest of the content model.
	 * <p>
	 * You are getting this "catch-all" property because of the following reason: The field name "PGPKeyPacket" is used by two different parts of a schema. See:
	 * line 174 of file:/home/user/ebms/ebms-core/src/main/resources/nl/clockwork/ebms/xsd/xmldsig-core-schema.xsd line 170 of
	 * file:/home/user/ebms/ebms-core/src/main/resources/nl/clockwork/ebms/xsd/xmldsig-core-schema.xsd
	 * <p>
	 * To get rid of this property, apply a property customization to one of both of the following declarations to change their names: Gets the value of the
	 * content property.
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
	 * Objects of the following type(s) are allowed in the list {@link Object } {@link JAXBElement }{@code <}{@link byte[]}{@code >} {@link JAXBElement
	 * }{@code <}{@link byte[]}{@code >} {@link Element }
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
