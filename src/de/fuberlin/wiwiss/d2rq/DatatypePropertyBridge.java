/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.datatypes.*;
import java.util.*;
import com.hp.hpl.jena.graph.impl.LiteralLabel;

/**
 * Representation of the d2rq:DatatypePropertyBridges from the mapping file.
 *
 * <BR>History: 06-03-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
public class DatatypePropertyBridge extends PropertyBridge  {
    private String column;
    private String pattern;
    private RDFDatatype datatype;
    private String lang;
    private Node refersToClassMap;
    private ArrayList joins;

    protected DatatypePropertyBridge(Node Id, Node prop, String col, String pat, RDFDatatype dataType, String Lang, ClassMap belongsTo, Node refers, ArrayList sqljoins, GraphD2RQ belongsToGraph) {
        id = Id;
        property = prop;
        column = col;
        pattern = pat;
        refersToClassMap = refers;
        joins = sqljoins;
        belongsToClassMap = belongsTo;
        d2rqGraph = belongsToGraph;
        datatype = dataType;
        lang = Lang;
    }

    protected String getColumn() { return column; }
    protected String getPattern() { return pattern; }
    protected String getLang() { return lang; }
    protected RDFDatatype getDatatype() { return datatype; }
    protected ClassMap getReferredClassMap() { return (ClassMap) d2rqGraph.classMaps.get(refersToClassMap); }
    protected ArrayList getJoins() { return joins; }

    /** Checks if a given node could fit the triple object without looking into the database */
    protected boolean nodeCouldFitObject(Node node) {

        // Node.ANY can allways fit
        if (node.equals(Node.ANY)) return true;

        // Check if node is a literal
        if (!node.isLiteral()) return false;

		LiteralLabel label = ((Node_Literal) node).getLiteral();

		// Check if node.datatype could fit the bridge
		if ((datatype != null && label.getDatatype() == null) ||
			(datatype == null && label.getDatatype() != null) ||
			(datatype != null &&
			 label.getDatatype() != null &&
			 !datatype.equals(label.getDatatype()))) {
				  return false;
		}

		// Check if node.languge could fit the bridge
		if ((lang != null && label.language().equals("")) ||
			(lang == null && !label.language().equals("")) ||
			(lang != null &&
			 !label.language().equals("") &&
			 !(label.language().equals(lang)))) {
				 return false;
		}

        // Check if local pattern could fit
        if ( pattern != null &&
			 D2RQUtil.valueCanFitPattern(label.getLexicalForm(), pattern)) {
                return true;

        // All nodes can fit a column value
        } else if (column != null ) {
        	return true;

        // Nothing of the above was true, thus return false
        } else {
            return false;
        }
    }

     /** Creates a node maker for the object of this bridge
      * and adds the nessesary SELECT and WHERE clauses to the SQLStatementMaker.
      * Node node is a condition given by the query or Node.ANY.
      *
      */
    protected NodeMaker getObjectMaker(Node node, SQLStatementMaker sqlMaker) {
        NodeMaker nodeMaker;
		if (column != null) {
			// Object created directly from column
			sqlMaker.addSelectColumn(column);
			nodeMaker = new LiteralMaker(null, column, null, datatype, lang);
		} else {
			// Object created with pattern
			Iterator columns = D2RQUtil.getColumnsfromPattern(pattern).iterator();
			while (columns.hasNext()) {
				String column = (String) columns.next();
				sqlMaker.addSelectColumn(column);
			}
			nodeMaker = new LiteralMaker(null, null, pattern, datatype, lang);
		}
		// Handle joins
		if  (joins != null) {
		   sqlMaker.addJoins(joins);
		}

		// OBJECT value set => create the WHERE clause
		if (node.isLiteral()) {
			  LiteralLabel label = ((Node_Literal) node).getLiteral();
			  if (pattern != null) {
					// Write pattern column names and values to WHERE clause
					HashMap columnsWithValues = D2RQUtil.ReverseValueWithPattern(label.getLexicalForm(), pattern);
					Iterator colIt = ((Set) columnsWithValues.keySet()).iterator();
					while (colIt.hasNext()) {
					   String key = (String) colIt.next();
					   String resultvalue = (String) columnsWithValues.get(key);
					   String whereClause = key + "=" +  D2RQUtil.getQuotedColumnValue(resultvalue, belongsToClassMap.getDatabase().getColumnType(key));
					   sqlMaker.addWhereClause(whereClause);
					}
			   } else if (column != null) {
					// Writecolumn name and value to WHERE clause
					String whereClause = column + "=" +  D2RQUtil.getQuotedColumnValue(label.getLexicalForm(), belongsToClassMap.getDatabase().getColumnType(column));
					sqlMaker.addWhereClause(whereClause);
			   }
		}
       return nodeMaker;
    }
}
