package de.fuberlin.wiwiss.d2rq.algebra;

/**
 * A database column.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Attribute.java,v 1.5 2006/09/15 15:31:23 cyganiak Exp $
 */
public class Attribute implements Comparable {
	private String attributeName;
	private RelationName relationName;
	private String qualifiedName;

	/**
	 * Constructs a new attribute from a schema name, table name
	 * and attribute name.
	 * @param schemaName The schema name, or <tt>null</tt> if not in a schema
	 * @param tableName The table name
	 * @param attributeName The column name
	 */
	public Attribute(String schemaName, String tableName, String attributeName) {
		this.qualifiedName = (schemaName == null)
				? tableName + "." + attributeName
				: schemaName + "." + tableName + "." + attributeName;
		this.relationName = new RelationName(schemaName, tableName);
		this.attributeName = attributeName;
	}
	
	/**
	 * Returns the column name in <tt>Table.Column</tt> form
	 * @return the column name
	 */
	public String qualifiedName() {
		return this.qualifiedName;
	}
	
	/**
	 * Extracts the database column name from a tablename.columnname
	 * combination.
	 * @return database column name.
	 */
	public String attributeName() {
		return this.attributeName;
	}

	/**
	 * Returns the database table name.
	 * @return database table name.
	 */
	public String tableName() {
		return this.relationName.tableName();
	}

	/**
	 * Returns the table name, including the schema if the table is
	 * in a schema.
	 * @return The table name in "table" or "schema.table" notation
	 */
	public RelationName relationName() {
		return this.relationName;
	}
	
	/**
	 * Extracts the database schema name from a schema.table.colum combination.
	 * @return database schema name, or <tt>null</tt> if no schema is specified.
	 */
	public String schemaName() {
		return this.relationName.schemaName();
	}

	public String toString() {
		return "@@" + this.qualifiedName + "@@";
	}
	
	/**
	 * Compares this instance to another object. Two Columns are considered
	 * equal when they have the same fully qualified name. We need this because
	 * we want to use Column instances as map keys.
	 * TODO: should not be equal if from different databases
	 */
	public boolean equals(Object other) {
		if (!(other instanceof Attribute)) {
			return false;
		}
		return this.qualifiedName.equals(((Attribute) other).qualifiedName());
	}

	/**
	 * Returns a hash code for this intance. We need this because
	 * we want to use Column instances as map keys.
	 * TODO: should be different for same-name columns from different databases
	 */
	public int hashCode() {
		return this.qualifiedName.hashCode();
	}

	/**
	 * Compares columns alphanumerically by qualified name, case sensitive.
	 * Attributes with schema are larger than attributes without schema.
	 */
	public int compareTo(Object otherObject) {
		if (!(otherObject instanceof Attribute)) {
			return 0;
		}
		Attribute other = (Attribute) otherObject;
		int i = this.relationName.compareTo(other.relationName);
		if (i != 0) {
			return i;
		}
		return this.attributeName.compareTo(other.attributeName);
	}
}