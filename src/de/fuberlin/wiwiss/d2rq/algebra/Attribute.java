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
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Attribute.java,v 1.2 2006/09/11 23:22:24 cyganiak Exp $
 */
public class Attribute implements Comparable {
	private static final java.util.regex.Pattern columnRegex = 
			java.util.regex.Pattern.compile("([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)\\.([a-zA-Z_]\\w*)");

	public static Set findColumnsInExpression(String expression) {
		Set results = new HashSet();
		Matcher match = columnRegex.matcher(expression);
		while (match.find()) {
			results.add(new Attribute(match.group(1), match.group(2)));
		}
		return results;
	}

	public static String replaceColumnsInExpression(String expression, ColumnRenamer columnRenamer) {
		StringBuffer result = new StringBuffer();
		Matcher match = columnRegex.matcher(expression);
		boolean matched = match.find();
		int firstPartEnd = matched ? match.start() : expression.length();
		result.append(expression.substring(0, firstPartEnd));
		while (matched) {
			Attribute column = new Attribute(match.group(1), match.group(2));
			result.append(columnRenamer.applyTo(column).qualifiedName());
			int nextPartStart = match.end();
			matched = match.find();
			int nextPartEnd = matched ? match.start() : expression.length();
			result.append(expression.substring(nextPartStart, nextPartEnd));
		}
		return result.toString();
	}
	
	private String attributeName;
	private String tableName;
	private String qualifiedName;

	/**
	 * Constructs a new Column from a fully qualified column name
	 * @param qualifiedName the column's name, for example <tt>Table.Column</tt>
	 */
	public Attribute(String qualifiedName) {
		Matcher match = columnRegex.matcher(qualifiedName);
		if (!match.matches()) {
			throw new D2RQException("\"" + qualifiedName + "\" is not in \"table.column\" notation");
		}
		this.qualifiedName = qualifiedName;
		this.tableName = match.group(1);
		this.attributeName =  match.group(2);
	}

	public Attribute(String tableName, String attributeName) {
		this.qualifiedName = tableName + "." + attributeName;
		this.tableName = tableName;
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
	 * Extracts the database table name from a tablename.columnname combination.
	 * @return database table name.
	 */
	public String tableName() {
		return this.tableName;
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
	 */
	public int compareTo(Object other) {
		if (!(other instanceof Attribute)) {
			return 0;
		}
		Attribute otherColumn = (Attribute) other;
		return qualifiedName().compareTo(otherColumn.qualifiedName());
	}
}