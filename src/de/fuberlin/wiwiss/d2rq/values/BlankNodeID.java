package de.fuberlin.wiwiss.d2rq.values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
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
 * @version $Id: BlankNodeID.java,v 1.3 2006/09/11 23:22:24 cyganiak Exp $
 */
public class BlankNodeID implements ValueMaker {
	private final static String DELIMITER = "@@";
	private String classMapID;
	private List attributes = new ArrayList(3);
	
	/**
	 * Constructs a new blank node identifier.
	 * @param columns a comma-seperated list of column names uniquely
	 * identifying the nodes
	 * @param classMapID a string that is unique for the class map
	 * whose resources are identified by this BlankNodeIdentifier 
	 */
	public BlankNodeID(String columns, String classMapID) {
		this.classMapID = classMapID;
		Iterator it = Arrays.asList(columns.split(",")).iterator();
		while (it.hasNext()) {
			this.attributes.add(new Attribute((String) it.next()));
		}
	}

	public void matchConstraint(NodeConstraint c) {
		c.matchBlankNodeIdentifier(this, this.attributes);
	}

	public boolean matches(String anonID) {
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
	public Set projectionAttributes() {
		return new HashSet(this.attributes);
	}

	/**
	 * Extracts column values from a blank node ID string. The
	 * keys are {@link Attribute}s, the values are strings.
	 * @param anonID value to be checked.
	 * @return a map with <tt>Column</tt> keys and string values
	 * @see de.fuberlin.wiwiss.d2rq.values.ValueMaker#attributeConditions(java.lang.String)
	 */
	public Map attributeConditions(String anonID) {
		String[] parts = anonID.split(DELIMITER);
		Map result = new HashMap(3);
		Iterator it = this.attributes.iterator();
		int i = 1;	// parts[0] is classMap identifier
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			result.put(attribute, parts[i]);
			i++;
		}
		return result;
	}

	/**
	 * Creates an identifier from a database row.
	 * @param row a database row
	 * @return this column's blank node identifier
	 */
	public String makeValue(ResultRow row) {
		StringBuffer result = new StringBuffer(this.classMapID);
		Iterator it = this.attributes.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			String value = row.get(attribute);
			if (value == null) {
				return null;
		    }
			result.append(DELIMITER);
			result.append(value);
		}
        return result.toString();
	}
	
	public ValueMaker replaceColumns(ColumnRenamer renamer) {
		StringBuffer columns = new StringBuffer();
		Iterator it = this.attributes.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			columns.append(renamer.applyTo(attribute).qualifiedName());
			if (it.hasNext()) {
				columns.append(",");
			}
		}
		return new BlankNodeID(columns.toString(), this.classMapID);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("BlankNodeID(");
		Iterator it = this.attributes.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			result.append(attribute.qualifiedName());
			if (it.hasNext()) {
				result.append(",");
			}
		}
		result.append(")");
		return result.toString();
	}
}