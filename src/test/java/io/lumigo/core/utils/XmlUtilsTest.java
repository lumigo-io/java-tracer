package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class XmlUtilsTest {

    private String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                    + "<PublishResponse xmlns=\"https://sns.amazonaws.com/doc/2010-03-31/\">"
                    + "<PublishResult>"
                    + "<MessageId>567910cd-659e-55d4-8ccb-5aaf14679dc0</MessageId>"
                    + "</PublishResult><ResponseMetadata><RequestId>d74b8436-ae13-5ab4-a9ff-ce54dfea72a0</RequestId>"
                    + "</ResponseMetadata>"
                    + "</PublishResponse>";

    @Test
    void test_xml_string_to_doc_not_valid_xml() {
        Document doc = XmlUtils.convertStringToDocument("NOT VALID");
        assertNull(doc);
    }

    @Test
    void test_xml_string_to_doc() {
        Document doc = XmlUtils.convertStringToDocument(xml);
        assertNotNull(doc);
    }

    @Test
    void test_xpath_search() {
        Document doc = XmlUtils.convertStringToDocument(xml);
        assertEquals(
                "567910cd-659e-55d4-8ccb-5aaf14679dc0",
                XmlUtils.getXpathFirstTextValue(
                        doc, "/PublishResponse/PublishResult/MessageId/text()"));
    }

    @Test
    void test_xpath_search_invalid() {
        Document doc = XmlUtils.convertStringToDocument(xml);
        assertNull(
                XmlUtils.getXpathFirstTextValue(
                        doc, "/PublishResponse/PublishResult/Not_exists/text()"));
    }
}
