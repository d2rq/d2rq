/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;

/**
 * D2RQ vocabulary terms as static objects.
 *
 * <BR>History: 06-03-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
public class D2RQ {

    /** D2RQnNamespace */
    protected static final String uri ="http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#";

    /** Deliminator used in patterns and bNode ids */
    public static final String deliminator = "@@";

    /** Returns the URI for this schema
        @return the URI for this schema
    */
    public static String getURI()
        { return uri; }

    // Database
    public static final Node Database = Node.createURI(uri + "Database");
    public static final Node odbcDSN = Node.createURI(uri + "odbcDSN");
    public static final Node jdbcDSN = Node.createURI(uri + "jdbcDSN");
    public static final Node jdbcDriver = Node.createURI(uri + "jdbcDriver");
    public static final Node username = Node.createURI(uri + "username");
    public static final Node password = Node.createURI(uri + "password");
    public static final Node numericColumn = Node.createURI(uri + "numericColumn");
    public static final Node textColumn = Node.createURI(uri + "textColumn");
    public static final Node dateColumn = Node.createURI(uri + "dateColumn");

    // ClassMap
    public static final Node ClassMap = Node.createURI(uri + "ClassMap");
    public static final Node uriPattern = Node.createURI(uri + "uriPattern");
    public static final Node uriColumn = Node.createURI(uri + "uriColumn");
    public static final Node bNodeIdColumns = Node.createURI(uri + "bNodeIdColumns");
    public static final Node dataStorage = Node.createURI(uri + "dataStorage");
    public static final Node classMap = Node.createURI(uri + "classMap");

    // PropertyBridge
    public static final Node DatatypePropertyBridge = Node.createURI(uri + "DatatypePropertyBridge");
    public static final Node ObjectPropertyBridge = Node.createURI(uri + "ObjectPropertyBridge");
    public static final Node column = Node.createURI(uri + "column");
    public static final Node join = Node.createURI(uri + "join");
    public static final Node pattern = Node.createURI(uri + "pattern");
    public static final Node belongsToClassMap = Node.createURI(uri + "belongsToClassMap");
    public static final Node refersToClassMap = Node.createURI(uri + "refersToClassMap");
    public static final Node datatype = Node.createURI(uri + "datatype");
    public static final Node lang = Node.createURI(uri + "lang");
    public static final Node propertyBridge = Node.createURI(uri + "propertyBridge");

}
