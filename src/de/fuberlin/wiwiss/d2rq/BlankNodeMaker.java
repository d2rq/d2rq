/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import java.util.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.AnonId;

/**
 * BlankNodeMakers transform attribute values from a result set into blank nodes.
 * They are used within TripleMakers.
 *
 * <BR>History: 06-21-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 *
 * @see de.fuberlin.wiwiss.d2rq.TripleMaker
 */
public class BlankNodeMaker extends NodeMaker {

    /** List of the column names that are used to construct a bNode id. */
    protected ArrayList bNodeIdColumns;
    /** ClassMap to which the nodeMaker belongs. */
    protected ClassMap classMap;

    protected BlankNodeMaker(ArrayList bNodeIdColumns,  ClassMap classMap) {
         this.bNodeIdColumns = bNodeIdColumns;
         this.classMap = classMap;
    }

    /** Creates a new blank node based on the current row of the result set
     * and the mapping of database column names to elements of the array.
     * Returns null if a NULL value was retrieved from the database.
    */
    protected Node getNode(String[] currentRow, HashMap columnNameNumberMap) {

		String nodeId = classMap.getId().toString();
		Iterator it =  bNodeIdColumns.iterator();
		while (it.hasNext()) {
			String fieldname = (String) it.next();
			int fieldNumber = Integer.parseInt((String) columnNameNumberMap.get(fieldname));
			nodeId +=  D2RQ.deliminator + currentRow[fieldNumber];
			if (currentRow[fieldNumber] == null) {
		    	return null;
		    }
		}
        return  Node.createAnon( new AnonId(nodeId));
    }
}
