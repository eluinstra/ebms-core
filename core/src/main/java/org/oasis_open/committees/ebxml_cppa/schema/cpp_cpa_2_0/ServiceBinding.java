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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Service"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}CanSend" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}CanReceive" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"service", "canSend", "canReceive"})
@XmlRootElement(name = "ServiceBinding")
public class ServiceBinding implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "Service", required = true)
	protected ServiceType service;
	@XmlElement(name = "CanSend")
	protected List<CanSend> canSend;
	@XmlElement(name = "CanReceive")
	protected List<CanReceive> canReceive;

	/**
	 * Gets the value of the service property.
	 * 
	 * @return possible object is {@link ServiceType }
	 */
	public ServiceType getService()
	{
		return service;
	}

	/**
	 * Sets the value of the service property.
	 * 
	 * @param value allowed object is {@link ServiceType }
	 */
	public void setService(ServiceType value)
	{
		this.service = value;
	}

	/**
	 * Gets the value of the canSend property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the canSend property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getCanSend().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link CanSend }
	 */
	public List<CanSend> getCanSend()
	{
		if (canSend == null)
		{
			canSend = new ArrayList<CanSend>();
		}
		return this.canSend;
	}

	/**
	 * Gets the value of the canReceive property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the canReceive property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getCanReceive().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link CanReceive }
	 */
	public List<CanReceive> getCanReceive()
	{
		if (canReceive == null)
		{
			canReceive = new ArrayList<CanReceive>();
		}
		return this.canReceive;
	}

}
