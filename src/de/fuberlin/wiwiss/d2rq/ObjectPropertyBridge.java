/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import java.util.*;

/** Representation of d2rq:ObjectPropertyBridges from the mapping file.
 *
 * <BR>History: 06-03-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
public class ObjectPropertyBridge extends PropertyBridge {
     private String column;
     private String pattern;
     private Node refersToClassMap;
     private ArrayList joins;

    protected ObjectPropertyBridge(Node Id, Node prop, String col, String pat, ClassMap belongsTo, Node refers, ArrayList sqljoins, GraphD2RQ belongsToGraph) {
        id = Id;
        property = prop;
        column = col;
        pattern = pat;
        belongsToClassMap = belongsTo;
        refersToClassMap = refers;
        joins = sqljoins;
        d2rqGraph = belongsToGraph;
    }
    protected String getColumn() { return column; }
    protected String getPattern() { return pattern; }
    protected ClassMap getReferredClassMap() { return (ClassMap) d2rqGraph.classMaps.get(refersToClassMap); }
    protected ArrayList getJoins() { return joins; }

    /** Checks if a given node could fit the triple object without looking into the database */
    protected boolean nodeCouldFitObject(Node node) {

        // Node.ANY can allways fit
        if (node.equals(Node.ANY)) return true;

        // Check if node is a literal
        if (node.isLiteral()) return false;

		if (node.isURI()) {
			////////////////////////////////////////////////////
			// Object is a URIref and a value is set
			////////////////////////////////////////////////////
			// Case 1: Check if value fits local column value
			// Case 2: Check if value fits local pattern value
			// Case 3: Check if value fits referred class uriColumn value
			// Case 4: Check if value fits referrred class uriPattern value
	
			if (refersToClassMap == null) {
				if (column != null) {
					// Case 1: Check if value fits local column value
					return true;
				} else if (pattern != null) {
					// Case 2: Check if value fits local pattern value
					return D2RQUtil.valueCanFitPattern(node.getURI(), pattern);
				} else {
                    // Nothing from the above fitted => return false
                    return false;
                }
			}
			ClassMap referredMap = getReferredClassMap();
			if (referredMap.getUriColumn() != null) {
				// All nodes can fit referred class uriColumn value
				return true;
			} else if (referredMap.getUriPattern() != null) {
			// Case 4: Check if value fits referrred class uriPattern value
				return D2RQUtil.valueCanFitPattern(node.getURI(), referredMap.getUriPattern());
			} else {
                // Nothing from the above fitted => return false
                return false;
            }

        ///////////////////////////////////////////
        // Object is bNode
        ///////////////////////////////////////////

		} else if (node.isBlank()) {
         	if (getReferredClassMap() == null) {
				return false;
			}
			if (getReferredClassMap().getbNodeIdColumns() == null) {
				return false;
			}
			// Check if given bNode fits D2RQ bNode construction
			String bNodeID = node.getBlankNodeId()	.toString();
			if (bNodeID.indexOf(D2RQ.deliminator) == -1) {
				return false;
			}

			// Check if given bNode was created by this bridge
			StringTokenizer tokenizer = new StringTokenizer(bNodeID, D2RQ.deliminator);
			return getReferredClassMap().getId().toString().equals(tokenizer.nextToken());
	   } else {
          // Node is strange
          return false;
       }
    }

     /** Creates a node maker for the object of this bridge
      * and adds the nessesary SELECT and WHERE clauses to the SQLStatementMaker.
      * Node node is a condition given by the query or Node.ANY.
      *
      */
    protected NodeMaker getObjectMaker(Node node, SQLStatementMaker sqlMaker) {
         NodeMaker nodeMaker = null;
			if (column != null) {
				// Object created directly from column
				sqlMaker.addSelectColumn(column);
				nodeMaker = new UriMaker(null, column, null);
			} else if (pattern != null) {
				// Object created with pattern
				Iterator columns = D2RQUtil.getColumnsfromPattern(pattern).iterator();
				while (columns.hasNext()) {
					String patternColumn = (String) columns.next();
					sqlMaker.addSelectColumn(patternColumn);
				}
				nodeMaker = new UriMaker(null, null, pattern);
			} else if (getReferredClassMap() != null) {
				 // Build URI of referred ClassMap instance
				 ClassMap referredMap = getReferredClassMap();
				 if (referredMap.getUriColumn() != null) {
					// Object created from referred URI column
					sqlMaker.addSelectColumn(referredMap.getUriColumn());
					nodeMaker = new UriMaker(null, referredMap.getUriColumn(), null);
				 } else if (referredMap.getUriPattern() != null) {
					// Object created with referred URI pattern
					String referredPattern = referredMap.getUriPattern();
					Iterator columns = D2RQUtil.getColumnsfromPattern(referredPattern).iterator();
					while (columns.hasNext()) {
						String patternColumn = (String) columns.next();
						sqlMaker.addSelectColumn(patternColumn);
					}
					nodeMaker = new UriMaker(null, null, referredPattern);
				 } else if (referredMap.getbNodeIdColumns() != null) {
					// Object created with referred bNode columns
					Iterator columns = referredMap.getbNodeIdColumns().iterator();
					while (columns.hasNext()) {
						String patternColumn = (String) columns.next();
						sqlMaker.addSelectColumn(patternColumn);
					}
					nodeMaker = new BlankNodeMaker(referredMap.getbNodeIdColumns(), referredMap);
				 }
			} else {
				nodeMaker = null;
				System.err.println("The bridge " + id.toString() + " has to have a d2rq:Pattern, d2rq:Column or d2rq:refersToClassMap clause.");
			}
			// Handle joins
			if  (joins != null) {
			   sqlMaker.addJoins(joins);
			}
            //////////////////////////////////////////////////////
			// OBJECT value set => create the WHERE clause
            //////////////////////////////////////////////////////
			if (!node.equals(Node.ANY)) {
				  if (node.isURI()) {
	
					////////////////////////////////////////////////////
					// Object is a URIref and a value is set
					////////////////////////////////////////////////////
					// Case 1: Value is local column value
					// Case 2: Value is local pattern value
					// Case 3: Value is referred class uriColumn value
					// Case 4: Value is referrred class uriPattern value
	
					if (getReferredClassMap() == null) {
						if (column != null) {
							// Case 1: Value is  local column value
							// Write column name and value to WHERE clause
							String whereClause = column + "=" +  D2RQUtil.getQuotedColumnValue(node.getURI(), belongsToClassMap.getDatabase().getColumnType(column));
							sqlMaker.addWhereClause(whereClause);
						} else {
							// Case 2: Value is local pattern value
							// Write pattern column names and values to WHERE clause
							HashMap columnsWithValues = D2RQUtil.ReverseValueWithPattern(node.getURI(), pattern);
							Iterator colIt = columnsWithValues.keySet().iterator();
							while (colIt.hasNext()) {
							   String key = (String) colIt.next();
							   String resultvalue = (String) columnsWithValues.get(key);
							   String whereClause = key + "=" +  D2RQUtil.getQuotedColumnValue(resultvalue, belongsToClassMap.getDatabase().getColumnType(key));
							   sqlMaker.addWhereClause(whereClause);
							}
						}
					} else {
						ClassMap referredMap = getReferredClassMap();
						if (referredMap.getUriColumn() != null) {
							// Case 3: Value is  referred class uriColumn value
							// Writecolumn name and value to WHERE clause
							String whereClause = referredMap.getUriColumn() + "=" +  D2RQUtil.getQuotedColumnValue(node.getURI(), referredMap.getDatabase().getColumnType(referredMap.getUriColumn()));
							sqlMaker.addWhereClause(whereClause);
						} else {
							// Case 4: Value is referrred class uriPattern value
							// Write pattern column names and values to WHERE clause
							HashMap columnsWithValues = D2RQUtil.ReverseValueWithPattern(node.getURI(), referredMap.getUriPattern());
							Iterator colIt = columnsWithValues.keySet().iterator();
							while (colIt.hasNext()) {
							   String key = (String) colIt.next();
							   String resultvalue = (String) columnsWithValues.get(key);
							   String whereClause = key + "=" +  D2RQUtil.getQuotedColumnValue(resultvalue, referredMap.getDatabase().getColumnType(key));
							   sqlMaker.addWhereClause(whereClause);
							}
						}
					}
			} else if (node.isBlank() && getReferredClassMap().getbNodeIdColumns() != null) {
				// Check if given bNode fits D2RQ bNode construction
				String bNodeID = node.getBlankNodeId().toString();
				StringTokenizer tokenizer = new StringTokenizer(bNodeID, D2RQ.deliminator);
				// Skip bridge name
				tokenizer.nextToken();
				Iterator bNodeColumnsIt = getReferredClassMap().getbNodeIdColumns().iterator();
				while (bNodeColumnsIt.hasNext()) {
					String columnName = (String) bNodeColumnsIt.next();
					String columnValue = tokenizer.nextToken();
					String whereClause = columnName + "=" + D2RQUtil.getQuotedColumnValue(columnValue, getReferredClassMap().getDatabase().getColumnType(columnName));
					sqlMaker.addWhereClause(whereClause);
				}
		   }
		}
        return nodeMaker;
    }
}
