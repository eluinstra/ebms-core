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
package nl.clockwork.ebms.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import lombok.var;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DOMUtils
{
	public static Schema createSchema(String xsdFile) throws SAXException
	{
		val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,"");
		//factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
		val stream = DOMUtils.class.getResourceAsStream(xsdFile);
		val systemId = DOMUtils.class.getResource(xsdFile).toString();
		return factory.newSchema(new StreamSource(stream,systemId));
	}

	public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException
	{
		return getDocumentBuilder(null);
	}

	public static DocumentBuilder getDocumentBuilder(Schema schema) throws ParserConfigurationException
	{
		val factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		factory.setSchema(schema);
		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setErrorHandler(new ErrorHandler()
		{
			@Override
			public void fatalError(SAXParseException exception) throws SAXException
			{
				log.fatal("",exception);
				throw exception;
			}
			@Override
			public void error(SAXParseException exception) throws SAXException
			{
				log.error("",exception);
				throw exception;
			}

			@Override
			public void warning(SAXParseException exception) throws SAXException
			{
				log.warn("",exception);
			}
		});
		return builder;
	}
	
	public static Transformer getTransformer() throws TransformerConfigurationException, TransformerFactoryConfigurationError
	{
		val result = createTransformerFactory().newTransformer();
		result.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		return result;
	}

	public static Transformer getTransformer(String xslFile) throws TransformerConfigurationException, TransformerFactoryConfigurationError
	{
		val result = createTransformerFactory().newTransformer(new StreamSource(DOMUtils.class.getResourceAsStream(xslFile)));
		result.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		return result;
	}

	private static TransformerFactory createTransformerFactory() throws TransformerFactoryConfigurationError
	{
		val result = TransformerFactory.newInstance();
		result.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
		result.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
		return result;
	}

	public static Element getFirstChildElement(Node node)
	{
		var child = node.getFirstChild();
		while ((child != null) && (child.getNodeType() != Node.ELEMENT_NODE))
			child = child.getNextSibling();
		return (Element)child;
	}
	
	public static Document read(String s) throws ParserConfigurationException, SAXException, IOException
	{
		return read((Schema)null,s);
	}

	public static Document read(Schema schema, String s) throws ParserConfigurationException, SAXException, IOException
	{
		val builder = getDocumentBuilder(schema);
		return builder.parse(new InputSource(new StringReader(s)));
	}

	public static Document read(String s, String encoding) throws ParserConfigurationException, SAXException, IOException
	{
		val builder = getDocumentBuilder();
		val source = new InputSource(new ByteArrayInputStream(s.getBytes(encoding)));
		source.setEncoding(encoding);
		return builder.parse(source);
	}

	public static Document read(InputStream stream) throws ParserConfigurationException, SAXException, IOException
	{
		return read(null,stream);
	}

	public static Document read(Schema schema, InputStream stream) throws ParserConfigurationException, SAXException, IOException
	{
		val builder = getDocumentBuilder(schema);
		return builder.parse(stream);
	}

	public static Document read(InputStream stream, String encoding) throws ParserConfigurationException, SAXException, IOException
	{
		val buidler = getDocumentBuilder();
		val in = new InputSource(stream);
		in.setEncoding(encoding);
		return buidler.parse(in);
	}

	public static String toString(Document document) throws TransformerException
	{
		val writer = new StringWriter();
		val transformer = getTransformer();
		transformer.transform(new DOMSource(document),new StreamResult(writer));
		return writer.toString();
	}

	public static String toString(Document document, String encoding) throws TransformerException
	{
		val writer = new StringWriter();
		val transformer = getTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING,encoding);
		transformer.transform(new DOMSource(document),new StreamResult(writer));
		return writer.toString();
	}

	public static void write(Document document, OutputStream outputStream) throws TransformerException
	{
		val transformer = getTransformer();
		transformer.transform(new DOMSource(document),new StreamResult(outputStream));
	}

	public static void write(Document document, OutputStream outputStream, String encoding) throws TransformerException
	{
		val transformer = getTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING,encoding);
		transformer.transform(new DOMSource(document),new StreamResult(outputStream));
	}

	public static void write(Document document, Writer writer) throws TransformerException
	{
		val transformer = getTransformer();
		transformer.transform(new DOMSource(document),new StreamResult(writer));
	}
	
	public static void write(Document document, Writer writer, String encoding) throws TransformerException
	{
		val transformer = getTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING,encoding);
		transformer.transform(new DOMSource(document),new StreamResult(writer));
	}
	
	public static Node executeXPathQuery(NamespaceContext namespaceContext, Document document, String query) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
		return (Node)executeXPathQuery(namespaceContext,document,query,XPathConstants.NODE);
	}

	public static Object executeXPathQuery(NamespaceContext namespaceContext, Document document, String query, QName returnType) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
		val xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(namespaceContext);
		val expr = xpath.compile(query);
		return (Node)expr.evaluate(document,returnType);
	}
}
