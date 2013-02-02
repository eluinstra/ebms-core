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

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DOMUtils
{
	public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		return dbf.newDocumentBuilder();
	}
	
	public static Transformer getTransformer() throws TransformerConfigurationException, TransformerFactoryConfigurationError
	{
		Transformer result = TransformerFactory.newInstance().newTransformer();
		//result.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"no");
		//result.setOutputProperty(OutputKeys.METHOD,"xml");
		//result.setOutputProperty(OutputKeys.INDENT,"yes");
		//result.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		return result;
	}

	public static Element getFirstChildElement(Node node)
	{
		Node child = node.getFirstChild();
		while ((child != null) && (child.getNodeType() != Node.ELEMENT_NODE))
			child = child.getNextSibling();
		return (Element)child;
	}
	
	public static Node getNode(Document document, String namespace, String tagName)
	{
		NodeList nl = document.getElementsByTagNameNS(namespace,tagName);
		return nl.getLength() == 0 ? null : nl.item(0);
	}

	public static String toString(Document document) throws TransformerException
	{
		//return document.getDocumentElement().toString();
		StringWriter writer = new StringWriter();
		Transformer transformer = getTransformer();
		transformer.transform(new DOMSource(document),new StreamResult(writer));
		return writer.toString();
	}

	public static void write(Document document, OutputStream outputStream) throws TransformerException
	{
		Transformer transformer = getTransformer();
		transformer.transform(new DOMSource(document),new StreamResult(outputStream));
	}

	public static void write(Document document, Writer writer) throws TransformerException
	{
		Transformer transformer = getTransformer();
		transformer.transform(new DOMSource(document),new StreamResult(writer));
	}

}
