/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.AnonId;
import java.util.*;

/**
     * Representation of a d2rq:ClassMap from the mapping file.
     * A ClassMap contains information from a d2rq:ClassMaps and is linked to a Database and to property bridges.
     *
     * <BR>History: 06-03-2004   : Initial version of this class.
     * @author Chris Bizer chris@bizer.de
     * @version V0.1
     */
public class ClassMap {

	/** Id of the classMap from the mapping file. */
    private Node id;
    /** RDFS Class which is linked to the classMap by an d2rq:classMap statement. */
    private Node rdfsClass;
    /** Collection of all property bridges which belong to this class map. */
    private HashSet propertyBridges;
    private String uriPattern;
    private String uriColumn;
    private Database database;
    private ArrayList bNodeIdColumns;

    public ClassMap(Node nodeId, Node rdfsclass, String pattern, String column, ArrayList bNodeIdColumns, Database data) {
        id = nodeId;
        rdfsClass = rdfsclass;
        uriPattern = pattern;
        uriColumn = column;
        database = data;
        this.bNodeIdColumns = bNodeIdColumns;
        propertyBridges = new HashSet();
        if (uriPattern == null &&
            uriColumn == null &&
            bNodeIdColumns == null ) {
           System.err.println("The classMap " + id.toString() +  " doesn't have a identifier constructor.");
        }
    }

	protected Node getId() { return id; }
	protected Node getrdfsClass() { return rdfsClass; }
	protected String getUriPattern() { return uriPattern; }
	protected String getUriColumn() { return uriColumn; }
    protected ArrayList getbNodeIdColumns() { return bNodeIdColumns; }
	protected Database getDatabase() { return database; }
    protected Iterator getPropertyBridgesIterator() {
        return propertyBridges.iterator(); }
    protected void addPropertyBridge(PropertyBridge bridge) {
        propertyBridges.add(bridge);
    }

    /** Creates a NodeMaker to produce instance ids for this classMap
      * and adds the nessesary SELECT clauses to the SQLStatementMaker.
      */
    protected NodeMaker getInstanceIdMaker(Node node, SQLStatementMaker sqlMaker) {
		NodeMaker nodeMaker;
		if (uriColumn != null) {
			// Node created with URI column
			sqlMaker.addSelectColumn(uriColumn);
			nodeMaker = new UriMaker(null, uriColumn, null);
		} else if (uriPattern != null) {
			// Node created with URI pattern
			String pattern = uriPattern;
			Iterator columns = D2RQUtil.getColumnsfromPattern(pattern).iterator();
			while (columns.hasNext()) {
				String column = (String) columns.next();
				sqlMaker.addSelectColumn(column);
			}
			nodeMaker = new UriMaker(null, null, pattern);
		} else {
			// Subject is a bNode
			Iterator columns = bNodeIdColumns.iterator();
			while (columns.hasNext()) {
				String column = (String) columns.next();
				sqlMaker.addSelectColumn(column);
			}
			nodeMaker = new BlankNodeMaker(bNodeIdColumns, this);
		}

        // Given node is not Node.ANY => add nessesary conditions to the WHERE clause
		if (!node.equals(Node.ANY)) {
			 if (uriPattern != null) {
				// Write pattern column names and values to WHERE clause
				HashMap columnsWithValues = D2RQUtil.ReverseValueWithPattern(node.getURI(), uriPattern);
				Iterator colIt = ((Set) columnsWithValues.keySet()).iterator();
				while (colIt.hasNext()) {
				   String key = (String) colIt.next();
				   String resultvalue = (String) columnsWithValues.get(key);
				   String whereClause = key + "=" +  D2RQUtil.getQuotedColumnValue(resultvalue, database.getColumnType(key));
				   sqlMaker.addWhereClause(whereClause);
				}
			} else if (uriColumn != null) {
				// Write column name and value to WHERE clause
				String whereClause = uriColumn + "=" +  D2RQUtil.getQuotedColumnValue(node.getURI(), database.getColumnType(uriColumn));
				sqlMaker.addWhereClause(whereClause);
			} else if (bNodeIdColumns != null) {
				// Write bNode id columns to WHERE clause
				String bNodeID = ((AnonId) node.getBlankNodeId()).toString();
				StringTokenizer tokenizer = new StringTokenizer(bNodeID, D2RQ.deliminator);
                Iterator bNodeColumnsIt = bNodeIdColumns.iterator();
                // Skip classMap name
                String classMapName = tokenizer.nextToken();
				while (bNodeColumnsIt.hasNext()) {
					String columnName = (String) bNodeColumnsIt.next();
					String columnValue = tokenizer.nextToken();
					String whereClause = columnName + "=" + D2RQUtil.getQuotedColumnValue(columnValue, database.getColumnType(columnName));
					sqlMaker.addWhereClause(whereClause);
				}
		   }

		}
        return nodeMaker;
    }

    /** Checks if a given node could fit the triple subject without querying the database */
    protected boolean nodeCouldBeInstanceId(Node node) {

        // Node.ANY can allways fit
        if (node.equals(Node.ANY)) return true;

        // Check if bNode could fit
        if (node.isBlank() && bNodeIdColumns != null) {

            String bNodeID = ((AnonId) node.getBlankNodeId()).toString();
            // Check if given bNode was created by D2RQ
			if (bNodeID.indexOf(D2RQ.deliminator) == -1) {
					return false;
			}
			StringTokenizer tokenizer = new StringTokenizer(bNodeID, D2RQ.deliminator);
			// Check if given bNode was created by this class map
			if (!((String) id.toString()).equals(tokenizer.nextToken())){
				   return false;
			} else {
                   return true;
            }

        // Check if URI could fit uri pattern
        } else if (node.isURI() && uriPattern != null) {
             if (D2RQUtil.valueCanFitPattern(node.getURI(), uriPattern)) {
                return true;
             } else {
                return false;
             }

        // All URIs could fit the uriColumn
        } else if (node.isURI() && uriColumn != null) {
            return true;
        // Nothing of the above was true, thus return false
        } else {
            return false;
        }
    }
}


