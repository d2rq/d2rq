package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.AttributeExpr;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * A database column.
 * 
 * TODO: Attribute should track its SQL datatype code
 * TODO: Attribute should track wether it is nullable
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Attribute implements Comparable, ProjectionSpec {
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
		this(new RelationName(schemaName, tableName), attributeName);
	}
	
	public Attribute(RelationName relationName, String attributeName) {
		this.attributeName = attributeName;
		this.relationName = relationName;
		this.qualifiedName = this.relationName.qualifiedName() + "." + this.attributeName;
	}
	
	/**
	 * Returns the column name in <tt>Table.Column</tt> form
	 * @return the column name
	 */
	public String qualifiedName() {
		return this.qualifiedName;
	}
	
	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return database.getSyntax().quoteAttribute(this);
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

	public Set requiredAttributes() {
		return Collections.singleton(this);
	}
	
	public Expression selectValue(String value) {
		return Equality.createAttributeValue(this, value);
	}
	
	public ProjectionSpec renameAttributes(ColumnRenamer renamer) {
		return renamer.applyTo(this);
	}
	
	public Expression toExpression() {
		return new AttributeExpr(this);
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