/*
 * $Id: BlankNodeIdentifier.java,v 1.1 2005/04/13 16:55:28 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import de.fuberlin.wiwiss.d2rq.TablePrefixer;

/**
 * A blank node identifier that uniquely identifies all resources generated from
 * a specific ClassMap.
 * <p>
 * (Note: The implementation makes some assumptions about the Column
 * class to keep the code simple and fast. This means BlankNodeIdentifier
 * might not work with some hypothetical subclasses of Column.)
 *
 * TODO: Write tests for matches, extractColumnValues, getValue
 * 
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
class BlankNodeIdentifier implements ValueSource, Prefixable {
	private String classMapID;
	private Set identifierColumns = new HashSet(3);
	
	public Object clone() throws CloneNotSupportedException {return super.clone();}
	public void prefixTables(TablePrefixer prefixer) {
		identifierColumns=prefixer.prefixSet(identifierColumns);
	}

	/**
	 * Constructs a new blank node identifier.
	 * @param columns a comma-seperated list of column names uniquely
	 * identifying the nodes
	 * @param classMapID a string that is unique for the class map
	 * whose resources are identified by this BlankNodeIdentifier 
	 */
	public BlankNodeIdentifier(String columns, String classMapID) {
		this.classMapID = classMapID;
		StringTokenizer tokenizer = new StringTokenizer(columns, ",");
		while (tokenizer.hasMoreTokens()) {
			this.identifierColumns.add(new Column(tokenizer.nextToken()));
		}
	}


	public boolean couldFit(String anonID) {
		int index = anonID.indexOf(D2RQ.deliminator);
		// Check if given bNode was created by D2RQ
		if (index == -1) {
			return false;
		}
		// Check if given bNode was created by this class map
		return this.classMapID.equals(anonID.substring(0, index));		
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.ValueSource#getColumns()
	 */
	public Set getColumns() {
		return this.identifierColumns;
	}

	/**
	 * Extracts column values from a blank node ID string. The
	 * keys are {@link Column}s, the values are strings.
	 * @param anonID value to be checked.
	 * @return a map with <tt>Column</tt> keys and string values
	 * @see de.fuberlin.wiwiss.d2rq.map.ValueSource#getColumnValues(java.lang.String)
	 */
	public Map getColumnValues(String anonID) {
		StringTokenizer tokenizer = new StringTokenizer(anonID, D2RQ.deliminator);
		// Skip classMap name
		tokenizer.nextToken();
		Map result = new HashMap(3);
		Iterator it = this.identifierColumns.iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			String value = tokenizer.nextToken();
			result.put(column, value);
		}
		return result;
	}

	/**
	 * Creates an identifier from a database row.
	 * @param row a database row
	 * @param columnNameNumberMap a map from qualified column names to indices
	 * 							into the row array
	 * @return this column's blank node identifier
	 */
	public String getValue(String[] row, Map columnNameNumberMap) {
		StringBuffer result = new StringBuffer(this.classMapID);
		Iterator it = this.identifierColumns.iterator();
		while (it.hasNext()) {
			String fieldName = ((Column) it.next()).getQualifiedName();
			int fieldIndex = ((Integer) columnNameNumberMap.get(fieldName)).intValue();
			if (row[fieldIndex] == null) {
				return null;
		    }
			result.append(D2RQ.deliminator);
			result.append(row[fieldIndex]);
		}
        return result.toString();
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("bNodeID: " + this.classMapID);
		Iterator it = this.identifierColumns.iterator();
		while (it.hasNext()) {
			result.append(D2RQ.deliminator);
			result.append(it.next());
		}
		return result.toString();
	}
}