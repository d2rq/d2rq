/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import java.util.*;

/** TripleMakers contain a subject, predicate and object maker
 * and produce triples using these makers.
 *
 * <BR>History: 06-10-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 *
 * @see de.fuberlin.wiwiss.d2rq.NodeMaker
 * @see de.fuberlin.wiwiss.d2rq.TripleResultSet
 */
public class TripleMaker {

    protected NodeMaker subjectMaker;
    protected NodeMaker predicateMaker;
    protected NodeMaker objectMaker;

    protected TripleMaker(NodeMaker subjectMaker, NodeMaker predicateMaker, NodeMaker objectMaker) {
            this.subjectMaker = subjectMaker;
            this.predicateMaker = predicateMaker;
            this.objectMaker = objectMaker;

    }

    protected Triple makeTriple(String[] currentRow, HashMap columnNameNumberMap) {
        Node subject = subjectMaker.getNode(currentRow, columnNameNumberMap );
        Node predicate = predicateMaker.getNode(currentRow, columnNameNumberMap);
        Node object = objectMaker.getNode(currentRow, columnNameNumberMap);
        if (subject == null || predicate == null || object == null) {
        	return null;
        }
        return new Triple(subject, predicate, object);

    }
}
