/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.vocabulary.RDF;

/** A RDFTypeProperty bridge is created for every d2rq:classMap statement in the mapping file.
 * The bridge is used for answering rdf:type queries.
 *
 * <BR>History: 06-03-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
public class RDFTypePropertyBridge extends PropertyBridge  {
    private Node rdfsClass;

    protected RDFTypePropertyBridge(Node Id, Node rdfsclass, ClassMap belongsTo, GraphD2RQ belongsToGraph) {
        id = Id;
        property = RDF.Nodes.type;
        rdfsClass = rdfsclass;
        belongsToClassMap = belongsTo;
        d2rqGraph = belongsToGraph;
    }
    protected Node getRdfsClass() { return rdfsClass; }

    /** Checks if a given node could fit the triple object without looking into the database */
    protected boolean nodeCouldFitObject(Node node) {
        if (node.equals(Node.ANY)) return true;
        return node.equals(rdfsClass);
    }
    /** Creates a node maker for the object of this bridge.
      */
    protected NodeMaker getObjectMaker(Node node, SQLStatementMaker sqlMaker) {
       return new UriMaker(rdfsClass, null, null);
    }
}

