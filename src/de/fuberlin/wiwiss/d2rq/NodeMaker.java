 /*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.Node;
import java.util.HashMap;

/**
 * Abstract class of all node makers.
 * NodeMakers transform attribute values from a result set into nodes.
 * They are used within TripleMakers.
 *
 * A node can be created from:
 * 
 * 1. A fixed value (URI/Literal)
 * 2. The value of a column in the database (URI/Literal)
 * 3. A pattern which includes one or more columns from the database (URI/Literal)
 * 4. One or more columns values that are used for the bNodeID (bNode)
 * 
 * <BR>History: 06-16-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 *
 * @see de.fuberlin.wiwiss.d2rq.TripleMaker
 * @see de.fuberlin.wiwiss.d2rq.UriMaker
 * @see de.fuberlin.wiwiss.d2rq.LiteralMaker
 * @see de.fuberlin.wiwiss.d2rq.BlankNodeMaker
 */
public abstract class NodeMaker {

    /** Fixed value which is returned by getNode(). */
    protected Node fixedNode;

    /** Creates a new node based on the information in the current row array
     * and the mapping of database column names to the elements of this array.
     * Overloaded in the subclasses of NodeMaker.
    */
    protected Node getNode(String[] currentRow, HashMap columnNameNumberMap) {
        return null;
    }
}
