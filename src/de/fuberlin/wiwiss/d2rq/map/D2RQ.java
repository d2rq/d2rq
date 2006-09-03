package de.fuberlin.wiwiss.d2rq.map;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * D2RQ vocabulary terms as static objects.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RQ.java,v 1.5 2006/09/03 00:08:10 cyganiak Exp $
 */
public class D2RQ {

    /** D2RQnNamespace */
    public static final String uri = "http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#";

    /** Deliminator used in patterns and bNode ids */
    public static final String deliminator = "@@";

    /** Returns the URI for this schema
        @return the URI for this schema
    */
    public static String getURI()
        { return uri; }
    
    // Processing
    public static final Node ProcessingInstructions = Node.createURI(uri + "ProcessingInstructions");
    public static final Node queryHandler = Node.createURI(uri + "queryHandler"); // className

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
    public static Node allowDistinct = Node.createURI(uri + "allowDistinct"); // true/false
    public static Node expressionTranslator = Node.createURI(uri + "expressionTranslator"); // className

    // ClassMap
    public static final Node ClassMap = Node.createURI(uri + "ClassMap");
    public static final Node uriPattern = Node.createURI(uri + "uriPattern");
    public static final Node uriColumn = Node.createURI(uri + "uriColumn");
    public static final Node bNodeIdColumns = Node.createURI(uri + "bNodeIdColumns");
    public static final Node dataStorage = Node.createURI(uri + "dataStorage");
    public static final Node containsDuplicates = Node.createURI(uri + "containsDuplicates");
    public static final Node classMap = Node.createURI(uri + "classMap");
    public static final Node class_ = Node.createURI(uri + "class");
    
    // PropertyBridge
    public static final Node DatatypePropertyBridge = Node.createURI(uri + "DatatypePropertyBridge");
    public static final Node ObjectPropertyBridge = Node.createURI(uri + "ObjectPropertyBridge");
    public static final Node column = Node.createURI(uri + "column");
    public static final Node join = Node.createURI(uri + "join");
    public static final Node alias = Node.createURI(uri + "alias"); // jg
    public static final Node pattern = Node.createURI(uri + "pattern");
    public static final Node belongsToClassMap = Node.createURI(uri + "belongsToClassMap");
    public static final Node refersToClassMap = Node.createURI(uri + "refersToClassMap");
    public static final Node datatype = Node.createURI(uri + "datatype");
    public static final Node lang = Node.createURI(uri + "lang");
    public static final Node translateWith = Node.createURI(uri + "translateWith");
    public static final Node valueMaxLength = Node.createURI(uri + "valueMaxLength");
    public static final Node valueContains = Node.createURI(uri + "valueContains");
    public static final Node valueRegex = Node.createURI(uri + "valueRegex");
    public static final Node propertyBridge = Node.createURI(uri + "propertyBridge");
    public static final Node property = Node.createURI(uri + "property");



    // ClassMap and PropertyBridge
    public static final Node condition = Node.createURI(uri + "condition");

    // AdditionalProperty
    public static final Node AdditionalProperty = Node.createURI(uri + "AdditionalProperty");
    public static final Node additionalProperty = Node.createURI(uri + "additionalProperty");
    public static final Node propertyName = Node.createURI(uri + "propertyName");
    public static final Node propertyValue = Node.createURI(uri + "propertyValue");

    // TranslationTable
    public static final Node TranslationTable = Node.createURI(uri + "TranslationTable");
    public static final Node href = Node.createURI(uri + "href");
    public static final Node javaClass = Node.createURI(uri + "javaClass");
    public static final Node translation = Node.createURI(uri + "translation");
    public static final Node Translation = Node.createURI(uri + "Translation");
    public static final Node databaseValue = Node.createURI(uri + "databaseValue");
    public static final Node rdfValue = Node.createURI(uri + "rdfValue");

    // D2RQ assembler for Jena
    public static final Resource D2RQModel = ResourceFactory.createResource(uri + "D2RQModel");
    public static final Property mappingFile = ResourceFactory.createProperty(uri + "mappingFile");
}