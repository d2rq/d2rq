package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A blank node identifier that uniquely identifies all resources generated from
 * a specific ClassMap.
 * <p>
 * (Note: The implementation makes some assumptions about the Column
 * class to keep the code simple and fast. This means BlankNodeIdentifier
 * might not work with some hypothetical subclasses of Column.)
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: BlankNodeIdentifier.java,v 1.9 2006/09/09 23:25:14 cyganiak Exp $
 */
public class BlankNodeIdentifier implements ValueSource {
	private final static String DELIMITER = "@@";
	private String classMapID;
	private List identifierColumns = new ArrayList(3);
	
	/**
	 * Constructs a new blank node identifier.
	 * @param columns a comma-seperated list of column names uniquely
	 * identifying the nodes
	 * @param classMapID a string that is unique for the class map
	 * whose resources are identified by this BlankNodeIdentifier 
	 */
	public BlankNodeIdentifier(String columns, String classMapID) {
		this.classMapID = classMapID;
		Iterator it = Arrays.asList(columns.split(",")).iterator();
		while (it.hasNext()) {
			this.identifierColumns.add(new Column((String) it.next()));
		}
	}

	public void matchConstraint(NodeConstraint c) {
		c.matchBlankNodeIdentifier(this, this.identifierColumns);
	}

	public boolean couldFit(String anonID) {
		int index = anonID.indexOf(DELIMITER);
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
		return new HashSet(this.identifierColumns);
	}

	/**
	 * Extracts column values from a blank node ID string. The
	 * keys are {@link Column}s, the values are strings.
	 * @param anonID value to be checked.
	 * @return a map with <tt>Column</tt> keys and string values
	 * @see de.fuberlin.wiwiss.d2rq.map.ValueSource#getColumnValues(java.lang.String)
	 */
	public Map getColumnValues(String anonID) {
		String[] parts = anonID.split(DELIMITER);
		Map result = new HashMap(3);
		Iterator it = this.identifierColumns.iterator();
		int i = 1;	// parts[0] is classMap identifier
		while (it.hasNext()) {
			Column column = (Column) it.next();
			result.put(column, parts[i]);
			i++;
		}
		return result;
	}

	/**
	 * Creates an identifier from a database row.
	 * @param row a database row
	 * @return this column's blank node identifier
	 */
	public String getValue(ResultRow row) {
		StringBuffer result = new StringBuffer(this.classMapID);
		Iterator it = this.identifierColumns.iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			String value = row.get(column);
			if (value == null) {
				return null;
		    }
			result.append(DELIMITER);
			result.append(value);
		}
        return result.toString();
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("BlankNodeID(");
		Iterator it = this.identifierColumns.iterator();
		while (it.hasNext()) {
			result.append(it.next());
			if (it.hasNext()) {
				result.append(",");
			}
		}
		result.append(")");
		return result.toString();
	}
}