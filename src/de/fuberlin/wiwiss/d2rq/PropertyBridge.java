/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;

/** Abstract superclass of all property bridges.
 * Subclassed by ObjectPropertyBridge, DatatypePropertyBridge
 * and RDFTypePropertyBridge.
 *
 * <BR>History: 06-03-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 *
 * @see de.fuberlin.wiwiss.d2rq.RDFTypePropertyBridge
 * @see de.fuberlin.wiwiss.d2rq.DatatypePropertyBridge
 * @see de.fuberlin.wiwiss.d2rq.ObjectPropertyBridge
 */
abstract public class PropertyBridge {

    /** Id of the PropertyBridge from the mapping file. */
    protected Node id;
    protected ClassMap belongsToClassMap;
    protected GraphD2RQ d2rqGraph;
    /** URI of the RDF predicate */
    protected Node property;

    protected ClassMap getClassMap() { return belongsToClassMap; }
    protected Node getProperty() { return property; }
    protected Node getId() { return id; }

    /** Checks if a given node could fit the triple subject without querying the database */
    protected boolean nodeCouldFitSubject(Node node) {
          return belongsToClassMap.nodeCouldBeInstanceId(node);
    }

    /** Checks if a given node could fit the triple predicate */
    protected boolean nodeCouldFitPredicate(Node node) {
        if (node.equals(Node.ANY)) return true;
        return node.equals(property);
    }

    /** Checks if a given node could fit the triple predicate.
      * This method is overloaded by the subclasses.
      */
    protected abstract boolean nodeCouldFitObject(Node node);

    /** Creates a node maker for the subject of this bridge
      * and adds the nessesary SELECT clauses to the SQLStatementMaker.
      * Node node is a condition a given by the query or Node.ANY.
      */
    protected NodeMaker getSubjectMaker(Node node, SQLStatementMaker sqlMaker) {
       return belongsToClassMap.getInstanceIdMaker(node, sqlMaker);
    }

    /** Creates a node maker for the predicate of this bridge */
    protected NodeMaker getPredicateMaker() {
       return new UriMaker(property, null, null);
    }

    /** Creates a node maker for the object of this bridge
      * and adds the nessesary SELECT and WHERE clauses to the SQLStatementMaker.
      * Node node is a condition a given by the query or Node.ANY.
      * This method is overloaded by the subclasses.
      */
    protected abstract NodeMaker getObjectMaker(Node node, SQLStatementMaker sqlMaker);
}