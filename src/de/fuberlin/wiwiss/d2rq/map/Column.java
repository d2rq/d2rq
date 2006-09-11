package de.fuberlin.wiwiss.d2rq.map;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.values.ValueSource;

/**
 * A database column.
 *
 * TODO: findColumnsInExpression and renameColumnsInExpression will fail
 *       e.g. for coumn names occuring inside string literals
 *       
 * TODO: Split into Column (the ValueSource part) and Attribute (the relation attribute part)?
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Column.java,v 1.10 2006/09/11 22:29:18 cyganiak Exp $
 */
public class Column implements ValueSource, Comparable {
	private static final java.util.regex.Pattern columnRegex = 
			java.util.regex.Pattern.compile("([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)\\.([a-zA-Z_]\\w*)");

	public static Set findColumnsInExpression(String expression) {
		Set results = new HashSet();
		Matcher match = columnRegex.matcher(expression);
		while (match.find()) {
			results.add(new Column(match.group(1), match.group(2)));
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
			Column column = new Column(match.group(1), match.group(2));
			result.append(columnRenamer.applyTo(column).getQualifiedName());
			int nextPartStart = match.end();
			matched = match.find();
			int nextPartEnd = matched ? match.start() : expression.length();
			result.append(expression.substring(nextPartStart, nextPartEnd));
		}
		return result.toString();
	}
	
	private String qualifiedName;
	private String tableName;
	private final String columnName;

	/**
	 * Constructs a new Column from a fully qualified column name
	 * @param qualifiedName the column's name, for example <tt>Table.Column</tt>
	 */
	public Column(String qualifiedName) {
		Matcher match = columnRegex.matcher(qualifiedName);
		if (!match.matches()) {
			throw new D2RQException("\"" + qualifiedName + "\" is not in \"table.column\" notation");
		}
		this.qualifiedName = qualifiedName;
		this.tableName = match.group(1);
		this.columnName =  match.group(2);
	}

	public Column(String tableName, String colName) {
		this.qualifiedName=tableName + "." + colName;
		this.tableName=tableName;
		this.columnName=colName;
	}
	
	public void matchConstraint(NodeConstraint c) {
		c.matchColumn(this);
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
		return this.columnName;
	}

	/**
	 * Extracts the database table name from a tablename.columnname combination.
	 * @return database table name.
	 */
	public String getTableName() {
		return this.tableName;
	}

	public boolean matches(String value) {
		return true;
	}

	public Set projectionAttributes() {
		return Collections.singleton(this);
	}

	public Map attributeConditions(String value) {
		return Collections.singletonMap(this, value);
	}

	/**
	 * Returns the value of this column from a database row.
	 * @param row a database row
	 * @return this column's value
	 */
	public String makeValue(ResultRow row) {
		return row.get(this);
	}
	
	public ValueSource replaceColumns(ColumnRenamer renamer) {
		return renamer.applyTo(this);
	}
	
	public String toString() {
		return "Column(" + this.qualifiedName + ")";
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

	/**
	 * Compares columns alphanumerically by qualified name, case sensitive.
	 */
	public int compareTo(Object other) {
		if (!(other instanceof Column)) {
			return 0;
		}
		Column otherColumn = (Column) other;
		return getQualifiedName().compareTo(otherColumn.getQualifiedName());
	}
}