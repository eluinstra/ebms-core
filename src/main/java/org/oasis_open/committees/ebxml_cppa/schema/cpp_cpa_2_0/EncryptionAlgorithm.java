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
import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for anonymous complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd>non-empty-string">
 *       &lt;attribute name="minimumStrength" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="oid" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}non-empty-string" />
 *       &lt;attribute name="w3c" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}non-empty-string" />
 *       &lt;attribute name="enumerationType" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}non-empty-string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"value"})
@XmlRootElement(name = "EncryptionAlgorithm")
public class EncryptionAlgorithm implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlValue
	protected String value;
	@XmlAttribute(name = "minimumStrength", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected BigInteger minimumStrength;
	@XmlAttribute(name = "oid", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected String oid;
	@XmlAttribute(name = "w3c", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected String w3C;
	@XmlAttribute(name = "enumerationType", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd")
	protected String enumerationType;

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
	 * Gets the value of the minimumStrength property.
	 * 
	 * @return possible object is {@link BigInteger }
	 */
	public BigInteger getMinimumStrength()
	{
		return minimumStrength;
	}

	/**
	 * Sets the value of the minimumStrength property.
	 * 
	 * @param value allowed object is {@link BigInteger }
	 */
	public void setMinimumStrength(BigInteger value)
	{
		this.minimumStrength = value;
	}

	/**
	 * Gets the value of the oid property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getOid()
	{
		return oid;
	}

	/**
	 * Sets the value of the oid property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setOid(String value)
	{
		this.oid = value;
	}

	/**
	 * Gets the value of the w3C property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getW3C()
	{
		return w3C;
	}

	/**
	 * Sets the value of the w3C property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setW3C(String value)
	{
		this.w3C = value;
	}

	/**
	 * Gets the value of the enumerationType property.
	 * 
	 * @return possible object is {@link String }
	 */
	public String getEnumerationType()
	{
		return enumerationType;
	}

	/**
	 * Sets the value of the enumerationType property.
	 * 
	 * @param value allowed object is {@link String }
	 */
	public void setEnumerationType(String value)
	{
		this.enumerationType = value;
	}

}
