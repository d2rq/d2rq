package de.fuberlin.wiwiss.d2rq.algebra;

/**
 * A relation name, including an optional schema name.
 *
 * TODO: Should know about its database and not be equal
 *       if the databases are not equal. (?)
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class RelationName implements Comparable<RelationName> {
	private String schemaName;
	private String tableName;
	private String qualifiedName;
	private boolean caseUnspecified;
	
	/**
	 * Constructs a new relation name.
	 * @param schemaName The schema name, or <tt>null</tt> if none
	 * @param tableName The table name
	 * @param caseUnspecified Whether the case is unspecified, i.e. comparisons on this relation name need to be case-insensitive
	 */
	public RelationName(String schemaName, String tableName, boolean caseUnspecified) {
		this.schemaName = schemaName;
		this.tableName = tableName;
		if (this.schemaName == null) {
			this.qualifiedName = tableName;
		} else {
			this.qualifiedName = schemaName + "." + tableName;
		}
		this.caseUnspecified = caseUnspecified;
	}
	
	/**
	 * Constructs a new relation name with specified case.
	 * @param schemaName The schema name, or <tt>null</tt> if none
	 * @param tableName The table name
	 */
	public RelationName(String schemaName, String tableName) {
		this(schemaName, tableName, false);
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
	
	public boolean caseUnspecified() {
		return this.caseUnspecified;
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
		
		if (this.caseUnspecified || other.caseUnspecified)
			return this.qualifiedName.equalsIgnoreCase(other.qualifiedName);
		else
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
	public int compareTo(RelationName other) {
		if (this.schemaName == null && other.schemaName == null) {
			return this.tableName.compareTo(other.tableName);
		}
		if (this.schemaName == null) {
			return -1;
		}
		if (other.schemaName == null) {
			return 1;
		}
		boolean caseUnspecified = this.caseUnspecified || other.caseUnspecified;
		int compareSchemas = caseUnspecified ? this.schemaName.compareToIgnoreCase(other.schemaName) : this.schemaName.compareTo(other.schemaName);
		if (compareSchemas != 0) {
			return compareSchemas;
		}
		return (caseUnspecified ? this.tableName.compareToIgnoreCase(other.tableName) : this.tableName.compareTo(other.tableName));
	}
	
	public RelationName withPrefix(int index) {
		
		String name = "T" + index + "_" + 
				(schemaName == null ? "" : schemaName + "_") +
				tableName ;
		
		/*
		 * Oracle can't handle identifier names longer than 30 characters.
		 * To prevent the oracle error "ORA-00972: identifier is too long"
		 * we need to cut those longer names off but keep them unique.
		 * 
		 * TODO: Make this dependent on whether we're dealing with an Oracle
		 * database or not.
		 */
		if (name.length() > 30) {
			name = "T" + index + "_" + name.hashCode() ;
		}
		
		return new RelationName(null, name);		
	}
}
