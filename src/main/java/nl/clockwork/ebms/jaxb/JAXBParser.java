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
package nl.clockwork.ebms.jaxb;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JAXBParser<T>
{
	private static HashMap<Class<?>,JAXBParser<?>> xmlHandlers = new HashMap<>();
	private static SAXParserFactory saxParserFactory;
	JAXBContext context;

	{
		try
		{
			saxParserFactory = SAXParserFactory.newInstance();
			saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		}
		catch (SAXNotRecognizedException | SAXNotSupportedException | ParserConfigurationException e)
		{
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public T handleUnsafe(String xml) throws JAXBException
	{
		if (StringUtils.isEmpty(xml))
			return null;
		val r = new StringReader(xml);
		val unmarshaller = context.createUnmarshaller();
		val o = unmarshaller.unmarshal(r);
		if (o instanceof JAXBElement<?>)
			return ((JAXBElement<T>)o).getValue();
		else
			return (T)o;
	}

	@SuppressWarnings("unchecked")
	public T handleUnsafe(Node n) throws JAXBException
	{
		if (n == null)
			return null;
		val unmarshaller = context.createUnmarshaller();
		val o = unmarshaller.unmarshal(n);
		if (o instanceof JAXBElement<?>)
			return ((JAXBElement<T>)o).getValue();
		else
			return (T)o;
	}

	@SuppressWarnings("unchecked")
	public T handle(String xml) throws JAXBException, SAXException, ParserConfigurationException
	{
		if (StringUtils.isEmpty(xml))
			return null;
		val parser = saxParserFactory.newSAXParser();
		val is = new InputSource(new StringReader(xml));
		val s = new SAXSource(parser.getXMLReader(),is);
		val unmarshaller = context.createUnmarshaller();
		val o = unmarshaller.unmarshal(s);
		if (o instanceof JAXBElement<?>)
			return ((JAXBElement<T>)o).getValue();
		else
			return (T)o;
	}

	public String handle(JAXBElement<T> e) throws JAXBException
	{
		if (e == null)
			return null;
		val result = new StringWriter();
		val marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.marshal(e,result);
		result.flush();
		return result.toString();
	}

	public String handle(JAXBElement<T> e, NamespacePrefixMapper namespacePrefixMapper) throws JAXBException
	{
		if (e == null)
			return null;
		val result = new StringWriter();
		val marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		if (namespacePrefixMapper != null)
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",namespacePrefixMapper);
		marshaller.marshal(e,result);
		result.flush();
		return result.toString();
	}

	public String handle(T object) throws JAXBException
	{
		if (object == null)
			return null;
		val result = new StringWriter();
		val marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.marshal(object,result);
		result.flush();
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	public static <L> JAXBParser<L> getInstance(Class<L> clazz) throws JAXBException
	{
		if (xmlHandlers.get(clazz) == null)
		{
			val context = JAXBContext.newInstance(clazz);
			xmlHandlers.put(clazz,new JAXBParser<L>(context));
		}
		return (JAXBParser<L>)xmlHandlers.get(clazz);
	}

	@SuppressWarnings("unchecked")
	public static <L> JAXBParser<L> getInstance(Class<L> clazz, Class<?>...clazzes) throws JAXBException
	{
		if (xmlHandlers.get(clazz) == null)
		{
			val context = JAXBContext.newInstance(clazzes);
			xmlHandlers.put(clazz,new JAXBParser<L>(context));
		}
		return (JAXBParser<L>)xmlHandlers.get(clazz);
	}

}