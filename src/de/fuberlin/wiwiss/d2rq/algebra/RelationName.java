package de.fuberlin.wiwiss.d2rq.algebra;


/**
 * A relation name, including an optional schema name.
 *
 * TODO: Should know about its database and not be equal
 *       if the databases are not equal. (?)
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RelationName.java,v 1.2 2006/09/15 15:31:23 cyganiak Exp $
 */
public class RelationName implements Comparable {
	private String schemaName;
	private String tableName;
	private String qualifiedName;
	
	/**
	 * Constructs a new relation name.
	 * @param schemaName The schema name, or <tt>null</tt> if none
	 * @param tableName The table name
	 */
	public RelationName(String schemaName, String tableName) {
		this.schemaName = schemaName;
		this.tableName = tableName;
		if (this.schemaName == null) {
			this.qualifiedName = tableName;
		} else {
			this.qualifiedName = schemaName + "." + tableName;
		}
	}
	
	/**
	 * @return The table name
	 */
	public String tableName() {
		return this.tableName;
	}
	
	/**
	 * @return The schema name, or <tt>null</tt> if none
	 */
	public String schemaName() {
		return this.schemaName;
	}
	
	/**
	 * Returns the full name, including the schema if present, in
	 * <tt>schema.table</tt> or <tt>table</tt> notation.
	 * @return The qualified name
	 */
	public String qualifiedName() {
		return this.qualifiedName;
	}
	
	public int hashCode() {
		return this.qualifiedName.hashCode();
	}
	
	/**
	 * Two relation names are identical if and only if
	 * they share the same name and schema, or they
	 * share the same name and both have no schema.
	 */
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof RelationName)) {
			return false;
		}
		RelationName other = (RelationName) otherObject;
		return this.qualifiedName.equals(other.qualifiedName);
	}
	
	public String toString() {
		return this.qualifiedName;
	}
	
	/**
	 * Relations without schema are less than relations with schema.
	 * Relations without schema are ordered by table name, those with
	 * schema are ordered by schema name.
	 */
	public int compareTo(Object otherObject) {
		if (!(otherObject instanceof RelationName)) {
			return 0;
		}
		RelationName other = (RelationName) otherObject;
		if (this.schemaName == null && other.schemaName == null) {
			return this.tableName.compareTo(other.tableName);
		}
		if (this.schemaName == null) {
			return -1;
		}
		if (other.schemaName == null) {
			return 1;
		}
		int compareSchemas = this.schemaName.compareTo(other.schemaName);
		if (compareSchemas != 0) {
			return compareSchemas;
		}
		return this.tableName.compareTo(other.tableName);
	}
}
