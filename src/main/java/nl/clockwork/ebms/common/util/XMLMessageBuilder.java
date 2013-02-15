/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.common.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class XMLMessageBuilder<T>
{
	private static HashMap<Class<?>,XMLMessageBuilder<?>> xmlHandlers = new HashMap<Class<?>,XMLMessageBuilder<?>>();
	private JAXBContext context;

	private XMLMessageBuilder(JAXBContext context)
	{
		this.context = context;
	}

	public T handle(String xml) throws JAXBException
	{
		if (StringUtils.isEmpty(xml))
			return null;
		return handle(new ByteArrayInputStream(xml.getBytes()));
	}

	@SuppressWarnings("unchecked")
	public T handle(InputStream is) throws JAXBException
	{
		if (is == null)
			return null;
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Object o = unmarshaller.unmarshal(is);
		if (o instanceof JAXBElement<?>)
			return (T)((JAXBElement<T>)o).getValue();
		else
			return (T)o;
	}

	@SuppressWarnings("unchecked")
	public T handle(Node n) throws JAXBException
	{
		if (n == null)
			return null;
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Object o = unmarshaller.unmarshal(n);
		if (o instanceof JAXBElement<?>)
			return (T)((JAXBElement<T>)o).getValue();
		else
			return (T)o;
	}

	public String handle(JAXBElement<T> e) throws JAXBException
	{
		if (e == null)
			return null;
		StringWriter result = new StringWriter();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.marshal(e,result);
		result.flush();
		return result.toString();
	}

	public String handle(JAXBElement<T> e, NamespacePrefixMapper namespacePrefixMapper) throws JAXBException
	{
		if (e == null)
			return null;
		StringWriter result = new StringWriter();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",namespacePrefixMapper);
		marshaller.marshal(e,result);
		result.flush();
		return result.toString();
	}

	public String handle(JAXBElement<T> e, @SuppressWarnings("restriction") com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper namespacePrefixMapper) throws JAXBException
	{
		if (e == null)
			return null;
		StringWriter result = new StringWriter();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper",namespacePrefixMapper);
		marshaller.marshal(e,result);
		result.flush();
		return result.toString();
	}

	public String handle(T object) throws JAXBException
	{
		if (object == null)
			return null;
		StringWriter result = new StringWriter();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.marshal(object,result);
		result.flush();
		return result.toString();
	}

	public String handle(T object, NamespacePrefixMapper namespacePrefixMapper) throws JAXBException
	{
		if (object == null)
			return null;
		StringWriter result = new StringWriter();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",namespacePrefixMapper);
		marshaller.marshal(object,result);
		result.flush();
		return result.toString();
	}

	public String handle(T object, @SuppressWarnings("restriction") com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper namespacePrefixMapper) throws JAXBException
	{
		if (object == null)
			return null;
		StringWriter result = new StringWriter();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper",namespacePrefixMapper);
		marshaller.marshal(object,result);
		result.flush();
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	public static <L> XMLMessageBuilder<L> getInstance(Class<L> clazz) throws JAXBException
	{
		if (xmlHandlers.get(clazz) == null)
		{
			JAXBContext context = JAXBContext.newInstance(clazz);
			xmlHandlers.put(clazz,new XMLMessageBuilder<L>(context));
		}
		return (XMLMessageBuilder<L>)xmlHandlers.get(clazz);
	}

	@SuppressWarnings("unchecked")
	public static <L> XMLMessageBuilder<L> getInstance(Class<L> clazz, Class<?>...clazzes) throws JAXBException
	{
		if (xmlHandlers.get(clazz) == null)
		{
			JAXBContext context = JAXBContext.newInstance(clazzes);
			xmlHandlers.put(clazz,new XMLMessageBuilder<L>(context));
		}
		return (XMLMessageBuilder<L>)xmlHandlers.get(clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> T deepCopy(T object) throws JAXBException
	{
		if (object == null)
			return null;
		JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
		JAXBElement<T> contentObject = new JAXBElement<T>(new QName(object.getClass().getSimpleName()),(Class<T>)object.getClass(),object);
		JAXBSource source = new JAXBSource(jaxbContext,contentObject);
		return jaxbContext.createUnmarshaller().unmarshal(source,(Class<T>)object.getClass()).getValue();
	}

}