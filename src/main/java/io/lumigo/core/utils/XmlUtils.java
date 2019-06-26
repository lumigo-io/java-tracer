package io.lumigo.core.utils;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.pmw.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlUtils {
    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static XPath xpath = XPathFactory.newInstance().newXPath();

    public static Document convertStringToDocument(String xmlStr) {
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            return doc;
        } catch (Exception e) {
            Logger.error(e, "Failed to convert {} to xml", xmlStr);
        }
        return null;
    }

    public static String getXpathFirstTextValue(Document document, String path) {
        try {
            XPathExpression expr = xpath.compile(path);
            return ((NodeList) expr.evaluate(document, XPathConstants.NODESET))
                    .item(0)
                    .getNodeValue();
        } catch (Exception e) {
            Logger.error(e, "Failed to get xpath {} from xml {}", path);
        }
        return null;
    }
}
