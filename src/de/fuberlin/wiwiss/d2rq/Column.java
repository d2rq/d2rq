/*
 * $Id: Column.java,v 1.1 2004/08/02 22:48:44 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A database column.
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
class Column implements ValueSource {
	private String qualifiedName;

	/**
	 * Constructs a new Column from a fully qualified column name
	 * @param qualifiedName the column's name, for example <tt>Table.Column</tt>
	 */
	public Column(String qualifiedName) {
		if (qualifiedName.indexOf('.') == -1) {
			Logger.instance().error("\"" + qualifiedName + "\" is not in \"table.column\" notation");
		}
		this.qualifiedName = qualifiedName;
	}

	/**
	 * Returns the column name in <tt>Table.Column</tt> form
	 * @return the column name
	 */
	public String getQualifiedName() {
		return this.qualifiedName;
	}

	/**
	 * Extracts the database column name from a tablename.columnname
	 * combination.
	 * @return database column name.
	 */
	public String getColumnName() {
		int dotIndex = this.qualifiedName.indexOf(".");
		return this.qualifiedName.substring(dotIndex + 1);
	}

	/**
	 * Extracts the database table name from a tablename.columnname combination.
	 * @return database table name.
	 */
	public String getTableName() {
		int dotIndex = this.qualifiedName.indexOf(".");
		return this.qualifiedName.substring(0, dotIndex);
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.ValueSource#couldFit(java.lang.String)
	 */
	public boolean couldFit(String value) {
		return true;
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.ValueSource#getColumns()
	 */
	public Set getColumns() {
		Set result = new HashSet(1);
		result.add(this);
		return result;
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.ValueSource#getColumnValues(java.lang.String)
	 */
	public Map getColumnValues(String value) {
		Map result = new HashMap(1);
		result.put(this, value);
		return result;
	}

	/**
	 * Returns the value of this column from a database row.
	 * 
	 * @param row
	 *            a database row
	 * @param columnNameNumberMap
	 *            a map from qualified column names to indices into the row
	 *            array
	 * @return this column's value
	 */
	public String getValue(String[] row, Map columnNameNumberMap) {
		Integer columnIndex = (Integer) columnNameNumberMap.get(this.qualifiedName); 
		return row[columnIndex.intValue()];
	}
	
	public String toString() {
		return this.qualifiedName;
	}
	
	/**
	 * Compares this instance to another object. Two Columns are considered
	 * equal when they have the same fully qualified name. We need this because
	 * we want to use Column instances as map keys.
	 * TODO: should not be equal if from different databases
	 */
	public boolean equals(Object other) {
		if (!(other instanceof Column)) {
			return false;
		}
		return this.qualifiedName.equals(((Column) other).getQualifiedName());
	}

	/**
	 * Returns a hash code for this intance. We need this because
	 * we want to use Column instances as map keys.
	 * TODO: should be different for same-name columns from different databases
	 */
	public int hashCode() {
		return this.qualifiedName.hashCode();
	}
}