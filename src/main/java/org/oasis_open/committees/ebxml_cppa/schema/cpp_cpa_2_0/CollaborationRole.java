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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ProcessSpecification"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Role"/>
 *         &lt;element name="ApplicationCertificateRef" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}CertificateRef.type" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="ApplicationSecurityDetailsRef" type="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}SecurityDetailsRef.type" minOccurs="0"/>
 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}ServiceBinding"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"processSpecification", "role", "applicationCertificateRef", "applicationSecurityDetailsRef", "serviceBinding"})
@XmlRootElement(name = "CollaborationRole")
public class CollaborationRole implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "ProcessSpecification", required = true)
	protected ProcessSpecification processSpecification;
	@XmlElement(name = "Role", required = true)
	protected Role role;
	@XmlElement(name = "ApplicationCertificateRef")
	protected List<CertificateRefType> applicationCertificateRef;
	@XmlElement(name = "ApplicationSecurityDetailsRef")
	protected SecurityDetailsRefType applicationSecurityDetailsRef;
	@XmlElement(name = "ServiceBinding", required = true)
	protected ServiceBinding serviceBinding;

	/**
	 * Gets the value of the processSpecification property.
	 * 
	 * @return possible object is {@link ProcessSpecification }
	 */
	public ProcessSpecification getProcessSpecification()
	{
		return processSpecification;
	}

	/**
	 * Sets the value of the processSpecification property.
	 * 
	 * @param value allowed object is {@link ProcessSpecification }
	 */
	public void setProcessSpecification(ProcessSpecification value)
	{
		this.processSpecification = value;
	}

	/**
	 * Gets the value of the role property.
	 * 
	 * @return possible object is {@link Role }
	 */
	public Role getRole()
	{
		return role;
	}

	/**
	 * Sets the value of the role property.
	 * 
	 * @param value allowed object is {@link Role }
	 */
	public void setRole(Role value)
	{
		this.role = value;
	}

	/**
	 * Gets the value of the applicationCertificateRef property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the applicationCertificateRef property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getApplicationCertificateRef().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link CertificateRefType }
	 */
	public List<CertificateRefType> getApplicationCertificateRef()
	{
		if (applicationCertificateRef == null)
		{
			applicationCertificateRef = new ArrayList<CertificateRefType>();
		}
		return this.applicationCertificateRef;
	}

	/**
	 * Gets the value of the applicationSecurityDetailsRef property.
	 * 
	 * @return possible object is {@link SecurityDetailsRefType }
	 */
	public SecurityDetailsRefType getApplicationSecurityDetailsRef()
	{
		return applicationSecurityDetailsRef;
	}

	/**
	 * Sets the value of the applicationSecurityDetailsRef property.
	 * 
	 * @param value allowed object is {@link SecurityDetailsRefType }
	 */
	public void setApplicationSecurityDetailsRef(SecurityDetailsRefType value)
	{
		this.applicationSecurityDetailsRef = value;
	}

	/**
	 * Gets the value of the serviceBinding property.
	 * 
	 * @return possible object is {@link ServiceBinding }
	 */
	public ServiceBinding getServiceBinding()
	{
		return serviceBinding;
	}

	/**
	 * Sets the value of the serviceBinding property.
	 * 
	 * @param value allowed object is {@link ServiceBinding }
	 */
	public void setServiceBinding(ServiceBinding value)
	{
		this.serviceBinding = value;
	}

}
