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
package org.xmlsoap.schemas.soap.envelope;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the org.xmlsoap.schemas.soap.envelope package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content. The Java representation of XML content can
 * consist of schema derived interfaces and classes representing the binding of schema type definitions, element declarations and model groups. Factory methods
 * for each of these are provided in this class.
 */
@XmlRegistry
public class ObjectFactory
{

	private final static QName _Fault_QNAME = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Fault");
	private final static QName _Header_QNAME = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Header");
	private final static QName _Envelope_QNAME = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Envelope");
	private final static QName _Body_QNAME = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.xmlsoap.schemas.soap.envelope
	 */
	public ObjectFactory()
	{
	}

	/**
	 * Create an instance of {@link Body }
	 */
	public Body createBody()
	{
		return new Body();
	}

	/**
	 * Create an instance of {@link Header }
	 */
	public Header createHeader()
	{
		return new Header();
	}

	/**
	 * Create an instance of {@link Envelope }
	 */
	public Envelope createEnvelope()
	{
		return new Envelope();
	}

	/**
	 * Create an instance of {@link Detail }
	 */
	public Detail createDetail()
	{
		return new Detail();
	}

	/**
	 * Create an instance of {@link Fault }
	 */
	public Fault createFault()
	{
		return new Fault();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Fault }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://schemas.xmlsoap.org/soap/envelope/", name = "Fault")
	public JAXBElement<Fault> createFault(Fault value)
	{
		return new JAXBElement<Fault>(_Fault_QNAME, Fault.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Header }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://schemas.xmlsoap.org/soap/envelope/", name = "Header")
	public JAXBElement<Header> createHeader(Header value)
	{
		return new JAXBElement<Header>(_Header_QNAME, Header.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Envelope }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://schemas.xmlsoap.org/soap/envelope/", name = "Envelope")
	public JAXBElement<Envelope> createEnvelope(Envelope value)
	{
		return new JAXBElement<Envelope>(_Envelope_QNAME, Envelope.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Body }{@code >}}
	 */
	@XmlElementDecl(namespace = "http://schemas.xmlsoap.org/soap/envelope/", name = "Body")
	public JAXBElement<Body> createBody(Body value)
	{
		return new JAXBElement<Body>(_Body_QNAME, Body.class, null, value);
	}

}
