/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import java.util.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.datatypes.*;

/**
 * LiteralMakers transform attribute values from a result set into literals.
 * They are used within TripleMakers.
 *
 * <BR>History: 06-21-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 *
 * @see de.fuberlin.wiwiss.d2rq.TripleMaker
 */
public class LiteralMaker extends NodeMaker {

    protected String nodeColumn;
    protected String nodePattern;
    protected RDFDatatype datatype;
    protected String lang;

    protected LiteralMaker(Node fixednode, String column, String pattern, RDFDatatype litDatatype, String litLang) {
         fixedNode = fixednode;
         nodeColumn = column;
         nodePattern = pattern;
         datatype = litDatatype;
         lang = litLang;
    }

    /** Creates a new literal node based on the current row of the result set
     * and the mapping of database column names to elements of the array.
     * Returns null if a NULL value was retrieved from the database.
    */
    protected Node getNode(String[] currentRow, HashMap columnNameNumberMap) {
        Node resultNode;
        if (fixedNode != null) {
            // Case: Fixed Node
            resultNode = fixedNode;
        } else if  (nodePattern != null) {
            // Case: Pattern
  		    String result = "";
			int startPosition = 0;
			int endPosition = 0;
			try {
				if (nodePattern.indexOf("@@") == -1) { result = nodePattern; }
                else {
					while (startPosition < nodePattern.length() && nodePattern.indexOf(D2RQ.deliminator, startPosition) != -1) {
						endPosition = startPosition;
						startPosition = nodePattern.indexOf(D2RQ.deliminator, startPosition);
						// get Text
						if (endPosition < startPosition)
							result += nodePattern.substring(endPosition, startPosition);
						startPosition = startPosition + D2RQ.deliminator.length();
						endPosition = nodePattern.indexOf(D2RQ.deliminator, startPosition);
						// get field
						String fieldname = nodePattern.substring(startPosition, endPosition).trim();
                        //String fnameShort = D2RQUtil.getColumnName(fieldname);
                        int fieldNumber = Integer.parseInt((String) columnNameNumberMap.get(fieldname));
                        if (currentRow[fieldNumber] == null) {
		    				return null;
		    			}
						result += currentRow[fieldNumber];
						startPosition = endPosition + D2RQ.deliminator.length();
					}
					if (endPosition + D2RQ.deliminator.length() < nodePattern.length())
						result += nodePattern.substring(startPosition, nodePattern.length());
			   }
            }
			catch (java.lang.Throwable ex) {
				System.err.println("Error: There was a problem while parsing the pattern" + nodePattern + ".");
            }
            resultNode = Node.createLiteral(result, lang, datatype);
        } else {
            // Case: Column
            int fieldNumber = Integer.parseInt((String) columnNameNumberMap.get(nodeColumn));
            String result = currentRow[fieldNumber];
            if (result == null) {
				return null;
			}
            resultNode = Node.createLiteral(result,  lang, datatype);
        }
        return resultNode;
    }
}
