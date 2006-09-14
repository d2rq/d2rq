package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * A database column.
 *
 * TODO: findColumnsInExpression and renameColumnsInExpression will fail
 *       e.g. for coumn names occuring inside string literals
 *
 * TODO: Extract a RelationName class that encapsulates table and schema name.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Attribute.java,v 1.3 2006/09/14 16:22:48 cyganiak Exp $
 */
public class Attribute implements Comparable {
	private static final java.util.regex.Pattern attributeRegex = 
			java.util.regex.Pattern.compile(
					// Optional schema name and dot, group 1 is schema name
					"(?:([a-zA-Z_]\\w*)\\.)?" +
					// Required table name and dot, group 2 is table name
					"([a-zA-Z_]\\w*)\\." +
					// Required column name, is group 3
					"([a-zA-Z_]\\w*)");

	public static Set findColumnsInExpression(String expression) {
		Set results = new HashSet();
		Matcher match = attributeRegex.matcher(expression);
		while (match.find()) {
			results.add(new Attribute(match.group(1), match.group(2), match.group(3)));
		}
		return results;
	}

	public static String replaceColumnsInExpression(String expression, ColumnRenamer columnRenamer) {
		StringBuffer result = new StringBuffer();
		Matcher match = attributeRegex.matcher(expression);
		boolean matched = match.find();
		int firstPartEnd = matched ? match.start() : expression.length();
		result.append(expression.substring(0, firstPartEnd));
		while (matched) {
			Attribute column = new Attribute(match.group(1), match.group(2), match.group(3));
			result.append(columnRenamer.applyTo(column).qualifiedName());
			int nextPartStart = match.end();
			matched = match.find();
			int nextPartEnd = matched ? match.start() : expression.length();
			result.append(expression.substring(nextPartStart, nextPartEnd));
		}
		return result.toString();
	}
	
	private String attributeName;
	private RelationName relationName;
	private String qualifiedName;

	/**
	 * Constructs a new Column from a fully qualified column name in <tt>Table.Column</tt>
	 * or <tt>Schema.Table.Column</tt> notation.
	 * 
	 * TODO: This constructor shouldn't be used except when parsing stuff from the mapping file
	 *       to reduce potential problems with funky column names. Move to a helper method closer to the parser?
	 * 
	 * @param qualifiedName The column's name
	 */
	public Attribute(String qualifiedName) {
		Matcher match = attributeRegex.matcher(qualifiedName);
		if (!match.matches()) {
			throw new D2RQException("\"" + qualifiedName + "\" is not in \"[schema.]table.column\" notation");
		}
		this.qualifiedName = qualifiedName;
		this.relationName = new RelationName(match.group(1), match.group(2));
		this.attributeName =  match.group(3);
	}

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