/**
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

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

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
	JAXBContext context;

	public T handle(String xml) throws JAXBException
	{
		return (handle(xml,null));
	}

	public T handle(String xml, Class<T> clazz) throws JAXBException
	{
		if (StringUtils.isEmpty(xml))
			return null;
		return handle(new StringReader(xml),clazz);
	}

	public T handle(InputStream is) throws JAXBException
	{
		return handle(null,is);
	}

	public T handle(Schema schema, InputStream is) throws JAXBException
	{
		return handle(schema,is,null);
	}

	public T handle(InputStream is, Class<T> clazz) throws JAXBException
	{
		return handle(null,is,clazz);
	}

	@SuppressWarnings("unchecked")
	public T handle(Schema schema, InputStream is, Class<T> clazz) throws JAXBException
	{
		if (is == null)
			return null;
		val unmarshaller = context.createUnmarshaller();
		if (schema != null)
			unmarshaller.setSchema(schema);
		val o = clazz == null ? unmarshaller.unmarshal(is) : unmarshaller.unmarshal(new StreamSource(is),clazz);
		if (o instanceof JAXBElement<?>)
			return ((JAXBElement<T>)o).getValue();
		else
			return (T)o;
	}

	public T handle(Reader r) throws JAXBException
	{
		return handle(null,r);
	}

	public T handle(Schema schema, Reader r) throws JAXBException
	{
		return handle(schema,r,null);
	}

	public T handle(Reader r, Class<T> clazz) throws JAXBException
	{
		return handle(null,r,clazz);
	}

	@SuppressWarnings("unchecked")
	public T handle(Schema schema, Reader r, Class<T> clazz) throws JAXBException
	{
		if (r == null)
			return null;
		val unmarshaller = context.createUnmarshaller();
		if (schema != null)
			unmarshaller.setSchema(schema);
		val o = clazz == null ? unmarshaller.unmarshal(r) : unmarshaller.unmarshal(new StreamSource(r),clazz);
		if (o instanceof JAXBElement<?>)
			return ((JAXBElement<T>)o).getValue();
		else
			return (T)o;
	}

	public T handle(XMLStreamReader r) throws JAXBException
	{
		return handle(null,r,null);
	}

	public T handle(Schema schema, XMLStreamReader r) throws JAXBException
	{
		return handle(schema,r,null);
	}

	public T handle(XMLStreamReader r, Class<T> clazz) throws JAXBException
	{
		return handle(null,r,clazz);
	}

	@SuppressWarnings("unchecked")
	public T handle(Schema schema, XMLStreamReader r, Class<T> clazz) throws JAXBException
	{
		if (r == null)
			return null;
		val unmarshaller = context.createUnmarshaller();
		if (schema != null)
			unmarshaller.setSchema(schema);
		val o = clazz == null ? unmarshaller.unmarshal(r) : unmarshaller.unmarshal(r,clazz);
		if (o instanceof JAXBElement<?>)
			return ((JAXBElement<T>)o).getValue();
		else
			return (T)o;
	}

	public T handle(Node n) throws JAXBException
	{
		return handle(null,n);
	}

	@SuppressWarnings("unchecked")
	public T handle(Schema schema, Node n) throws JAXBException
	{
		if (n == null)
			return null;
		val unmarshaller = context.createUnmarshaller();
		if (schema != null)
			unmarshaller.setSchema(schema);
		val o = unmarshaller.unmarshal(n);
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

	public String handle(T object, NamespacePrefixMapper namespacePrefixMapper) throws JAXBException
	{
		if (object == null)
			return null;
		val result = new StringWriter();
		val marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",namespacePrefixMapper);
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