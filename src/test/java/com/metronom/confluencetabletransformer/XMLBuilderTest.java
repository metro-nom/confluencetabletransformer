package com.metronom.confluencetabletransformer;

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLBuilderTest {

    private static final DocumentBuilderFactory DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private static Document createTestDocumentWithRoot() throws ParserConfigurationException {
        final DocumentBuilder documentBuilder = XMLBuilderTest.DOCUMENT_FACTORY.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();
        final Element rootElement = document.createElement("root");
        document.appendChild(rootElement);
        return document;
    }

    private static String toString(final Document document) throws TransformerException {
        final Transformer transformer = XMLBuilderTest.TRANSFORMER_FACTORY.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        final DOMSource source = new DOMSource(document);
        final StringWriter writer = new StringWriter();
        final StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        return writer.toString();
    }

    @Test
    public void createPageWithTable()
    throws ParserConfigurationException, ParseException, TransformerException {
        final List<TestObject> objects = new ArrayList<TestObject>();
        objects.add(new TestObject("Sam", 23));
        objects.add(new TestObject("Mary", 21));
        final Document document = ConfluenceXMLBuilder.createPageWithTable(objects, new TestObjectTransformer());
        Assert.assertEquals(
            XMLBuilderTest.toString(document),
            "<table class=\"wrapped\"><colgroup><col/><col/></colgroup><tbody><tr><th>name</th><th>age</th></tr>"
            + "<tr><td>Sam</td><td>23</td></tr><tr><td>Mary</td><td>21</td></tr></tbody></table>"
        );
    }

    @Test
    public void createXMLContentWrapperMultipleNodes()
    throws ParserConfigurationException, ParseException, TransformerException {
        final Document document = XMLBuilderTest.createTestDocumentWithRoot();
        final List<Node> children =
            Arrays.asList(
                document.createTextNode("prefix"),
                document.createElement("br"),
                document.createTextNode("suffix")
            );
        document.getDocumentElement().appendChild(ConfluenceXMLBuilder.createXMLContentWrapper(document, children));
        Assert.assertEquals(
            XMLBuilderTest.toString(document),
            "<root><div class=\"content-wrapper\"><p>prefix<br/>suffix</p></div></root>"
        );
    }

    @Test
    public void createXMLContentWrapperSingleNode()
    throws ParserConfigurationException, ParseException, TransformerException {
        final Document document = XMLBuilderTest.createTestDocumentWithRoot();
        document.getDocumentElement().appendChild(
            ConfluenceXMLBuilder.createXMLContentWrapper(document, document.createTextNode("test"))
        );
        Assert.assertEquals(
            XMLBuilderTest.toString(document),
            "<root><div class=\"content-wrapper\"><p>test</p></div></root>"
        );
    }

    @Test
    public void createXMLNodeForDate() throws ParserConfigurationException, ParseException, TransformerException {
        final Document document = XMLBuilderTest.createTestDocumentWithRoot();
        final DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        final Date date = format.parse("1984-08-13");
        document.getDocumentElement().appendChild(ConfluenceXMLBuilder.createXMLNodeForDate(document, date));
        Assert.assertEquals(XMLBuilderTest.toString(document), "<root><time datetime=\"1984-08-13\"/></root>");
    }

    private static class TestObject {

        public final String name;

        public final int age;

        public TestObject(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

    }

    private static class TestObjectTransformer implements RowObjectTransformer<TestObject> {

        @Override
        public TestObject parseRowFromHeadingMap(final Map<String, Node> nodes) {
            return
                new TestObject(
                    nodes.get("name").getTextContent(),
                    ConfluenceTableParser.parseInt(nodes.get("age").getTextContent())
                );
        }

        @Override
        public List<String> getTableHeadings() {
            return Arrays.asList("name", "age");
        }

        @Override
        public Node toConfluenceXMLRow(final Document document, final TestObject object) {
            final Element row = ConfluenceXMLBuilder.createRow(document);
            row.appendChild(ConfluenceXMLBuilder.createTextColumn(document, object.name));
            row.appendChild(ConfluenceXMLBuilder.createTextColumn(document, "" + object.age));
            return row;
        }

    }

}
