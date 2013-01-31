package nl.clockwork.common.util;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DOMUtils
{
	public static Element getFirstChildElement(Node node)
	{
		Node child = node.getFirstChild();
		while ((child != null) && (child.getNodeType() != Node.ELEMENT_NODE))
			child = child.getNextSibling();
		return (Element)child;
	}
	
	public static String toString(Document document) throws TransformerException
	{
		// return document.getDocumentElement().toString();
		StringWriter writer = new StringWriter();
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		//transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"no");
		//transformer.setOutputProperty(OutputKeys.METHOD,"xml");
		//transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		//transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		transformer.transform(new DOMSource(document),new StreamResult(writer));
		return writer.toString();
	}

	public static void write(Document document, OutputStream outputStream) throws TransformerException
	{
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		//transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"no");
		//transformer.setOutputProperty(OutputKeys.METHOD,"xml");
		//transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		//transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		transformer.transform(new DOMSource(document),new StreamResult(outputStream));
	}

	public static void write(Document document, Writer writer) throws TransformerException
	{
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		//transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"no");
		//transformer.setOutputProperty(OutputKeys.METHOD,"xml");
		//transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		//transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		transformer.transform(new DOMSource(document),new StreamResult(writer));
	}

}
