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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
 *         &lt;element name="ProcessingCapabilities">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="parse" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *                 &lt;attribute name="generate" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="CompositeList" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;choice maxOccurs="unbounded">
 *                   &lt;element name="Encapsulation">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Constituent"/>
 *                           &lt;/sequence>
 *                           &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}pkg.grp"/>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                   &lt;element name="Composite">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Constituent" maxOccurs="unbounded"/>
 *                           &lt;/sequence>
 *                           &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}pkg.grp"/>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/choice>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}id use="required""/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"processingCapabilities", "compositeList"})
@XmlRootElement(name = "Packaging")
public class Packaging implements Serializable
{

	private final static long serialVersionUID = 1L;
	@XmlElement(name = "ProcessingCapabilities", required = true)
	protected Packaging.ProcessingCapabilities processingCapabilities;
	@XmlElement(name = "CompositeList", required = true)
	protected List<Packaging.CompositeList> compositeList;
	@XmlAttribute(name = "id", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlID
	@XmlSchemaType(name = "ID")
	protected String id;

	/**
	 * Gets the value of the processingCapabilities property.
	 * 
	 * @return possible object is {@link Packaging.ProcessingCapabilities }
	 */
	public Packaging.ProcessingCapabilities getProcessingCapabilities()
	{
		return processingCapabilities;
	}

	/**
	 * Sets the value of the processingCapabilities property.
	 * 
	 * @param value allowed object is {@link Packaging.ProcessingCapabilities }
	 */
	public void setProcessingCapabilities(Packaging.ProcessingCapabilities value)
	{
		this.processingCapabilities = value;
	}

	/**
	 * Gets the value of the compositeList property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside
	 * the JAXB object. This is why there is not a <CODE>set</CODE> method for the compositeList property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getCompositeList().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Packaging.CompositeList }
	 */
	public List<Packaging.CompositeList> getCompositeList()
	{
		if (compositeList == null)
		{
			compositeList = new ArrayList<Packaging.CompositeList>();
		}
		return this.compositeList;
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
	 * <p>
	 * Java class for anonymous complex type.
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 * 
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;choice maxOccurs="unbounded">
	 *         &lt;element name="Encapsulation">
	 *           &lt;complexType>
	 *             &lt;complexContent>
	 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *                 &lt;sequence>
	 *                   &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Constituent"/>
	 *                 &lt;/sequence>
	 *                 &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}pkg.grp"/>
	 *               &lt;/restriction>
	 *             &lt;/complexContent>
	 *           &lt;/complexType>
	 *         &lt;/element>
	 *         &lt;element name="Composite">
	 *           &lt;complexType>
	 *             &lt;complexContent>
	 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *                 &lt;sequence>
	 *                   &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Constituent" maxOccurs="unbounded"/>
	 *                 &lt;/sequence>
	 *                 &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}pkg.grp"/>
	 *               &lt;/restriction>
	 *             &lt;/complexContent>
	 *           &lt;/complexType>
	 *         &lt;/element>
	 *       &lt;/choice>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {"encapsulationOrComposite"})
	public static class CompositeList implements Serializable
	{

		private final static long serialVersionUID = 1L;
		@XmlElements({@XmlElement(name = "Encapsulation", type = Packaging.CompositeList.Encapsulation.class),
				@XmlElement(name = "Composite", type = Packaging.CompositeList.Composite.class)})
		protected List<Serializable> encapsulationOrComposite;

		/**
		 * Gets the value of the encapsulationOrComposite property.
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present
		 * inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the encapsulationOrComposite property.
		 * <p>
		 * For example, to add a new item, do as follows:
		 * 
		 * <pre>
		 * getEncapsulationOrComposite().add(newItem);
		 * </pre>
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link Packaging.CompositeList.Encapsulation } {@link Packaging.CompositeList.Composite }
		 */
		public List<Serializable> getEncapsulationOrComposite()
		{
			if (encapsulationOrComposite == null)
			{
				encapsulationOrComposite = new ArrayList<Serializable>();
			}
			return this.encapsulationOrComposite;
		}

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
		 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Constituent" maxOccurs="unbounded"/>
		 *       &lt;/sequence>
		 *       &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}pkg.grp"/>
		 *     &lt;/restriction>
		 *   &lt;/complexContent>
		 * &lt;/complexType>
		 * </pre>
		 */
		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "", propOrder = {"constituent"})
		public static class Composite implements Serializable
		{

			private final static long serialVersionUID = 1L;
			@XmlElement(name = "Constituent", required = true)
			protected List<Constituent> constituent;
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
			 * Gets the value of the constituent property.
			 * <p>
			 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present
			 * inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the constituent property.
			 * <p>
			 * For example, to add a new item, do as follows:
			 * 
			 * <pre>
			 * getConstituent().add(newItem);
			 * </pre>
			 * <p>
			 * Objects of the following type(s) are allowed in the list {@link Constituent }
			 */
			public List<Constituent> getConstituent()
			{
				if (constituent == null)
				{
					constituent = new ArrayList<Constituent>();
				}
				return this.constituent;
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
		 *         &lt;element ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}Constituent"/>
		 *       &lt;/sequence>
		 *       &lt;attGroup ref="{http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd}pkg.grp"/>
		 *     &lt;/restriction>
		 *   &lt;/complexContent>
		 * &lt;/complexType>
		 * </pre>
		 */
		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "", propOrder = {"constituent"})
		public static class Encapsulation implements Serializable
		{

			private final static long serialVersionUID = 1L;
			@XmlElement(name = "Constituent", required = true)
			protected Constituent constituent;
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
			 * Gets the value of the constituent property.
			 * 
			 * @return possible object is {@link Constituent }
			 */
			public Constituent getConstituent()
			{
				return constituent;
			}

			/**
			 * Sets the value of the constituent property.
			 * 
			 * @param value allowed object is {@link Constituent }
			 */
			public void setConstituent(Constituent value)
			{
				this.constituent = value;
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

	}

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
	 *       &lt;attribute name="parse" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
	 *       &lt;attribute name="generate" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "")
	public static class ProcessingCapabilities implements Serializable
	{

		private final static long serialVersionUID = 1L;
		@XmlAttribute(name = "parse", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
		protected boolean parse;
		@XmlAttribute(name = "generate", namespace = "http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd", required = true)
		protected boolean generate;

		/**
		 * Gets the value of the parse property.
		 */
		public boolean isParse()
		{
			return parse;
		}

		/**
		 * Sets the value of the parse property.
		 */
		public void setParse(boolean value)
		{
			this.parse = value;
		}

		/**
		 * Gets the value of the generate property.
		 */
		public boolean isGenerate()
		{
			return generate;
		}

		/**
		 * Sets the value of the generate property.
		 */
		public void setGenerate(boolean value)
		{
			this.generate = value;
		}

	}

}
