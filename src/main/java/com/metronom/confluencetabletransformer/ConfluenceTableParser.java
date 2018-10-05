package com.metronom.confluencetabletransformer;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

import org.w3c.dom.*;
import org.w3c.dom.Node;

public class ConfluenceTableParser<T> {

    public static final DateFormat CONFLUENCE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final NodeList EMPTY_NODE_LIST =
        new NodeList() {

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Node item(final int index) {
                return null;
            }

        };

    private static final Logger LOG = Logger.getLogger(ConfluenceTableParser.class.getName());

    private static List<String> getHeadersFromTable(final Node table) {
        final Node firstChild = table.getFirstChild();
        final NodeList headerNodes;
        if ("tr".equals(firstChild.getNodeName())) {
            headerNodes = firstChild.getChildNodes();
        } else {
            final Node lastChild = table.getLastChild();
            if ("tbody".equals(lastChild.getNodeName())) {
                final Node firstRow = lastChild.getFirstChild();
                if ("tr".equals(firstRow.getNodeName())) {
                    headerNodes = firstRow.getChildNodes();
                } else {
                    headerNodes = ConfluenceTableParser.EMPTY_NODE_LIST;
                }
            } else {
                headerNodes = ConfluenceTableParser.EMPTY_NODE_LIST;
            }
        }
        final List<String> headers = new ArrayList<String>();
        for (int i = 0; i < headerNodes.getLength(); i++) {
            headers.add(headerNodes.item(i).getTextContent());
        }
        return headers;
    }

    public static List<String> getTableHeadersFromDocument(final Document document) {
        final NodeList tables = document.getElementsByTagName("table");
        if (tables.getLength() != 1) {
            ConfluenceTableParser.LOG.log(Level.SEVERE, "Document does not have exactly one table!");
            return Collections.emptyList();
        }
        return ConfluenceTableParser.getHeadersFromTable(tables.item(0));
    }

    public static void getTextAndTimeContent(final Node node, final StringBuilder builder) {
        if ("time".equals(node.getNodeName())) {
            builder.append(node.getAttributes().getNamedItem("datetime").getNodeValue());
            return;
        }
        if ("#text".equals(node.getNodeName())) {
            builder.append(node.getTextContent());
            return;
        }
        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            ConfluenceTableParser.getTextAndTimeContent(children.item(i), builder);
        }
    }

    public static Date parseDate(final DateFormat manualFormat, final String text) {
        final String toParse = ConfluenceTableParser.trimAlsoNonBreaking(text);
        if ("".equals(toParse)) {
            return null;
        }
        try {
            return manualFormat.parse(toParse);
        } catch (final ParseException e1) {
            try {
                return ConfluenceTableParser.CONFLUENCE_DATE_FORMAT.parse(toParse);
            } catch (final ParseException e2) {
                ConfluenceTableParser.LOG.log(Level.WARNING, String.format("Could not parse date %s!", text));
                return null;
            }
        }
    }

    public static double parseDouble(final String text) {
        final String check = ConfluenceTableParser.trimAlsoNonBreaking(text).toLowerCase();
        if ("".equals(check)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(check);
        } catch (final NumberFormatException e) {
            ConfluenceTableParser.LOG.log(Level.WARNING, "Could not parse double!", e);
            return 0.0;
        }
    }

    public static int parseInt(final String text) {
        final String check = ConfluenceTableParser.trimAlsoNonBreaking(text).toLowerCase();
        if ("".equals(check)) {
            return 0;
        }
        try {
            return Integer.parseInt(check);
        } catch (final NumberFormatException e) {
            ConfluenceTableParser.LOG.log(Level.WARNING, "Could not parse int!", e);
            return 0;
        }
    }

    public static long parseLong(final String text) {
        final String check = ConfluenceTableParser.trimAlsoNonBreaking(text).toLowerCase();
        if ("".equals(check)) {
            return 0;
        }
        try {
            return Long.parseLong(check);
        } catch (final NumberFormatException e) {
            ConfluenceTableParser.LOG.log(Level.WARNING, "Could not parse long!", e);
            return 0;
        }
    }

    public static String trimAlsoNonBreaking(final String string) {
        String previous = string;
        String current = previous.trim().replaceAll("(^\\h*)|(\\h*$)","");
        while (!current.equals(previous)) {
            previous = current;
            current = previous.trim().replaceAll("(^\\h*)|(\\h*$)","");
        }
        return current;
    }

    private final Document document;

    private final RowObjectTransformer<T> rowObjectTransformer;

    public ConfluenceTableParser(final Document document, final RowObjectTransformer<T> rowObjectTransformer) {
        this.document = document;
        this.rowObjectTransformer = rowObjectTransformer;
    }

    private List<Node> getRows(final Node table) {
        final Node lastChild = table.getLastChild();
        final NodeList rows;
        if ("tbody".equals(lastChild.getNodeName())) {
            rows = lastChild.getChildNodes();
        } else {
            rows = table.getChildNodes();
        }
        final List<Node> result = new ArrayList<Node>();
        for (int i = 1; i < rows.getLength(); i++) {
            result.add(rows.item(i));
        }
        return result;
    }

    public List<T> parse() throws IOException {
        final NodeList tables = this.document.getElementsByTagName("table");
        final List<T> result = new ArrayList<T>();
        for (int i = 0; i < tables.getLength(); i++) {
            result.addAll(this.parseTable(tables.item(i)));
        }
        return result;
    }

    private T parseRow(final List<String> headers, final Node row) {
        final NodeList cols = row.getChildNodes();
        if (cols.getLength() != headers.size()) {
            ConfluenceTableParser.LOG.log(Level.SEVERE, "Number of columns does not match number of headers!");
            ConfluenceTableParser.LOG.log(Level.SEVERE, row.getTextContent());
            return null;
        }
        final Map<String, Node> colsMap = new LinkedHashMap<String, Node>();
        for (int i = 0; i < cols.getLength(); i++) {
            colsMap.put(headers.get(i), cols.item(i));
        }
        return this.rowObjectTransformer.parseRowFromHeadingMap(colsMap);
    }

    private Collection<T> parseTable(final Node table) {
        final List<String> headers = ConfluenceTableParser.getHeadersFromTable(table);
        final List<String> normalizedHeaders =
            headers
            .stream()
            .map(header -> ConfluenceTableParser.trimAlsoNonBreaking(header).toLowerCase())
            .collect(Collectors.toList());
        if (!normalizedHeaders.containsAll(this.rowObjectTransformer.getTableHeadings())) {
            ConfluenceTableParser.LOG.log(Level.SEVERE, "Table does not contain all mandatory headers!");
            return Collections.emptyList();
        }
        final List<Node> rows = this.getRows(table);
        return
            rows
            .stream()
            .map(row -> this.parseRow(headers, row))
            .filter(row -> row != null)
            .collect(Collectors.toList());
    }

}
