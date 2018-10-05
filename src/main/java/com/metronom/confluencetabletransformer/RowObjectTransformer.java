package com.metronom.confluencetabletransformer;

import java.util.*;

import org.w3c.dom.*;

/**
 * Transformer between rows of a confluence table (given by a mapping from table headers to XML nodes) and java objects.
 * @author Thomas Stroeder
 * @param <T> The object to read from/write to a confluence table.
 */
public interface RowObjectTransformer<T> {

    /**
     * @param nodes Mapping from table headers to XML nodes containing the data for parsing the object.
     * @return The parsed object.
     */
    T parseRowFromHeadingMap(Map<String, Node> nodes);

    /**
     * @return The table headers in the confluence table.
     */
    List<String> getTableHeadings();

    /**
     * @param document The XML document which should contain the confluence table.
     * @param object The object to transform to a row in the confluence table.
     * @return The XML node representing the row of a confluence table containing the data of the specified object.
     */
    Node toConfluenceXMLRow(Document document, T object);

}
