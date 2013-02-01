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

import java.io.IOException;
import java.io.StringReader;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;

public class XMLUtils
{
	public static String objectToXML(Object o)
	{
		XStream xstream = new XStream();
		//xstream.alias("object",o.getClass());
		return xstream.toXML(o);
	}

	public static Object xmlToObject(String xml)
	{
		XStream xstream = new XStream();
		return xstream.fromXML(xml);
	}

	public static String executeXPathQuery(String content, String query) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return executeXPathQuery(builder.parse(new InputSource(new StringReader(content))),query);
	}
	
	public static String executeXPathQuery(Node node, String query) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
		return (String)executeXPathQuery(node,query,XPathConstants.STRING);
	}
	
	public static Object executeXPathQuery(Node node, String query, QName returnType) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile(query);
		
		return expr.evaluate(node,returnType);
	}
	
	public static String executeXPathQuery(NamespaceContext namespaceContext, String content, String query) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    return executeXPathQuery(namespaceContext,builder.parse(new InputSource(new StringReader(content))),query);
	}

	public static String executeXPathQuery(NamespaceContext namespaceContext, Node node, String query) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
	    return (String)executeXPathQuery(node,query,XPathConstants.STRING);
	}

	public static Object executeXPathQuery(NamespaceContext namespaceContext, Node node, String query, QName returnType) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
	    XPath xpath = XPathFactory.newInstance().newXPath();
	    xpath.setNamespaceContext(namespaceContext);
	    XPathExpression expr = xpath.compile(query);
	
//		Object result = expr.evaluate(document,XPathConstants.NODESET);
//		NodeList nodes = (NodeList) result;
//		for (int i = 0; i < nodes.getLength(); i++)
//			System.out.println(nodes.item(i).getNodeValue()); 
	
	    return expr.evaluate(node,returnType);
	}

}
