package com.metronom.confluencetabletransformer;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ConfluenceXMLBuilder {

    private static final DocumentBuilderFactory DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();

    public static Node createColumn(final Document document, final List<Node> children) {
        final Node result = document.createElement("td");
        for (final Node child : children) {
            result.appendChild(child);
        }
        return result;
    }

    public static Node createColumn(final Document document, final Node child) {
        return ConfluenceXMLBuilder.createColumn(document, Collections.singletonList(child));
    }

    public static Node createEmptyColumn(final Document document) {
        return ConfluenceXMLBuilder.createColumn(document, document.createElement("br"));
    }

    public static <T> Document createPageWithTable(
        final Collection<T> objects,
        final RowObjectTransformer<T> parser
    ) throws ParserConfigurationException {
        if (objects.isEmpty()) {
            return null;
        }
        final Document document = ConfluenceXMLBuilder.DOCUMENT_FACTORY.newDocumentBuilder().newDocument();
        final Element table = document.createElement("table");
        table.setAttribute("class", "wrapped");
        final int numOfCols = parser.getTableHeadings().size();
        final Node colgroup = document.createElement("colgroup");
        for (int i = 0; i < numOfCols; i++) {
            colgroup.appendChild(document.createElement("col"));
        }
        table.appendChild(colgroup);
        final Node body = document.createElement("tbody");
        final Node headerRow = document.createElement("tr");
        for (final String header : parser.getTableHeadings()) {
            final Node headerColumn = document.createElement("th");
            headerColumn.appendChild(document.createTextNode(header));
            headerRow.appendChild(headerColumn);
        }
        body.appendChild(headerRow);
        table.appendChild(body);
        document.appendChild(table);
        for (final T object : objects) {
            body.appendChild(parser.toConfluenceXMLRow(document, object));
        }
        return document;
    }

    public static Element createRow(final Document document) {
        return document.createElement("tr");
    }

    public static Node createTextColumn(final Document document, final String text) {
        return
            text == null || "".equals(text) ?
                ConfluenceXMLBuilder.createEmptyColumn(document) :
                    ConfluenceXMLBuilder.createColumn(document, document.createTextNode(text));
    }

    public static Node createXMLColumnForSingleDate(final Document document, final Date date) {
        if (date == null) {
            return ConfluenceXMLBuilder.createEmptyColumn(document);
        }
        return
            ConfluenceXMLBuilder.createColumn(
                document,
                ConfluenceXMLBuilder.createXMLContentWrapper(
                    document,
                    ConfluenceXMLBuilder.createXMLNodeForDate(document, date)
                )
            );
    }

    public static Node createXMLContentWrapper(final Document document, final List<Node> content) {
        final Element result = document.createElement("div");
        result.setAttribute("class", "content-wrapper");
        final Node paragraph = document.createElement("p");
        for (final Node child : content) {
            paragraph.appendChild(child);
        }
        result.appendChild(paragraph);
        return result;
    }

    public static Node createXMLContentWrapper(final Document document, final Node content) {
        return ConfluenceXMLBuilder.createXMLContentWrapper(document, Collections.singletonList(content));
    }

    public static Node createXMLNodeForDate(final Document document, final Date date) {
        final Element result = document.createElement("time");
        result.setAttribute("datetime", ConfluenceTableParser.CONFLUENCE_DATE_FORMAT.format(date));
        return result;
    }

}
