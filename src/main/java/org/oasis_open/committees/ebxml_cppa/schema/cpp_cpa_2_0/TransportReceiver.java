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
 *         &lt;element name="TransportProtocol" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}protocol.type"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}AccessAuthentication" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Endpoint" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}TransportServerSecurity" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"transportProtocol", "accessAuthentication", "endpoint", "transportServerSecurity"})
@XmlRootElement(name = "TransportReceiver")
public class TransportReceiver implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "TransportProtocol", required = true)
	protected ProtocolType transportProtocol;
	@XmlElement(name = "AccessAuthentication")
	protected List<AccessAuthenticationType> accessAuthentication;
	@XmlElement(name = "Endpoint", required = true)
	protected List<Endpoint> endpoint;
	@XmlElement(name = "TransportServerSecurity")
	protected TransportServerSecurity transportServerSecurity;

	/**
	 * Gets the value of the transportProtocol property.
	 * 
	 * @return possible object is {@link ProtocolType }
	 */
	public ProtocolType getTransportProtocol()
	{
		return transportProtocol;
	}

	/**
	 * Sets the value of the transportProtocol property.
	 * 
	 * @param value allowed object is {@link ProtocolType }
	 */
	public void setTransportProtocol(ProtocolType value)
	{
		this.transportProtocol = value;
	}

	/**
	 * Gets the value of the accessAuthentication property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the accessAuthentication property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getAccessAuthentication().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link AccessAuthenticationType }
	 */
	public List<AccessAuthenticationType> getAccessAuthentication()
	{
		if (accessAuthentication == null)
		{
			accessAuthentication = new ArrayList<AccessAuthenticationType>();
		}
		return this.accessAuthentication;
	}

	/**
	 * Gets the value of the endpoint property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the endpoint property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getEndpoint().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Endpoint }
	 */
	public List<Endpoint> getEndpoint()
	{
		if (endpoint == null)
		{
			endpoint = new ArrayList<Endpoint>();
		}
		return this.endpoint;
	}

	/**
	 * Gets the value of the transportServerSecurity property.
	 * 
	 * @return possible object is {@link TransportServerSecurity }
	 */
	public TransportServerSecurity getTransportServerSecurity()
	{
		return transportServerSecurity;
	}

	/**
	 * Sets the value of the transportServerSecurity property.
	 * 
	 * @param value allowed object is {@link TransportServerSecurity }
	 */
	public void setTransportServerSecurity(TransportServerSecurity value)
	{
		this.transportServerSecurity = value;
	}

}
