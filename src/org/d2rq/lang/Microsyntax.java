package org.d2rq.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.d2rq.D2RQException;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.expr.SQLExpression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;


/**
 * Static methods for handling various microsyntaxes used in the D2RQ
 * mapping language.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Microsyntax {

	/**
	 * Parses a comma-separated list of column names, e.g., for bNodeIdColumns
	 */
	public static List<ColumnName> parseColumnList(String commaSeparated) {
		List<ColumnName> result = new ArrayList<ColumnName>();
		for (String column: Arrays.asList(commaSeparated.split(","))) {
			result.add(Microsyntax.parseColumn(column.trim()));
		}
		return result;
	}
	
	/**
	 * Constructs a table name from a fully qualified name in <tt>[[catalog.]schema.]table</tt>
	 * notation. Delimiters may not be used.
	 */
	public static TableName parseTable(String s) {
		Matcher match = relationNameRegex.matcher(s);
		if (!match.matches()) {
			throw new D2RQException("Table name \"" + s + 
					"\" is not in \"[schema.]table\" notation",
					D2RQException.SQL_INVALID_RELATIONNAME);
		}
		return TableName.create(null, 
				Identifier.createDelimited(match.group(1)),
				Identifier.createDelimited(match.group(2)));
	}

	/**
	 * Returns a [schema.]table string representation. No quoting or escaping.
	 * Suitable for use in D2RQ mapping files, but not for SQL.
	 */
	public static String toString(TableName tableName) {
		return (tableName.getSchema() == null ? "" : tableName.getSchema().getName() + ".") + 
				tableName.getTable().getName();
	}
	
	/**
	 * Returns a [schema.]table.column string representation. No quoting or escaping.
	 * Suitable for use in D2RQ mapping files, but not for SQL.
	 */
	public static String toString(TableName tableName, Identifier column) {
		return (tableName == null ? "" : toString(tableName) + ".") + column.getName();
	}
	
	/**
	 * Returns a [schema.]table.column string representation. No quoting or escaping.
	 * Suitable for use in D2RQ mapping files, but not for SQL.
	 */
	public static String toString(ColumnName column) {
		return (column.isQualified() ? toString(column.getQualifier()) + "." : "") + column.getColumn().getName();
	}
	/**
	 * Parses a column in [Schema.]Table.Column notation.
	 * Allows no quotes or escaping. Allows no catalog.
	 * Assumes case sensitive. Must be qualified with a table name.
	 */
	public static ColumnName parseColumn(String s) {
		Matcher match = attributeRegexLax.matcher(s);
		if (!match.matches()) {
			throw new D2RQException("Column name \"" + s + 
					"\" is not in \"[schema.]table.column\" notation",
					D2RQException.SQL_INVALID_ATTRIBUTENAME);
		}
		return createColumn(match.group(1), match.group(2), match.group(3));
	}

	/**
	 * Parses a SQL expression, such as "table1.foo > 0", and returns a
	 * corresponding {@link Expression}. Column names must be qualified
	 * with table (and optional schema), and must be given without
	 * quotes or escapes. Some effort is being made to avoid misidentifying
	 * columns inside string literals, but it's safest to write the expression
	 * so that it doesn't contain anything looking like a qualified column name
	 * elsewhere. 
	 */
	public static Expression parseSQLExpression(String expression, DataType.GenericType dataType) {
		List<String> literalParts = new ArrayList<String>();
		List<ColumnName> columns = new ArrayList<ColumnName>();
		Matcher match = attributeRegexConservative.matcher(expression);
		boolean matched = match.find();
		int firstPartEnd = matched ? (match.start(1) != -1 ? match.start(1)
				   				    					   : match.start(2))
				   				   : expression.length();
		literalParts.add(expression.substring(0, firstPartEnd));
		while (matched) {
			columns.add(createColumn(match.group(1), match.group(2), match.group(3)));
			int nextPartStart = match.end();
			matched = match.find();
			int nextPartEnd = matched ? (match.start(1) != -1 ? match.start(1)
					  										  : match.start(2))
					  				  : expression.length();
			literalParts.add(expression.substring(nextPartStart, nextPartEnd));
		}
		return SQLExpression.create(literalParts, columns, dataType);
	}
	
	/**
	 * Assumes schema, table, column names without quotes or escape
	 * characters. Will yield a reference to column "p1"."p2"."p3"
	 * in standard SQL, with p1 and p2 optional.
	 */
	public static ColumnName createColumn(String p1, String p2, String p3) {
		return ColumnName.create(null, Identifier.createDelimited(p1),
				Identifier.createDelimited(p2), Identifier.createDelimited(p3));
	}

	private static final java.util.regex.Pattern attributeRegexConservative = 
		java.util.regex.Pattern.compile(
				// Ignore quoted text since the last match or the beginning of the string;
				// taking inner string escaping into consideration
				// This is required to distinguish similar strings
				// like file and host names (e.g. in URLs) from column names
				"\\G[^']*?(?:'[^'\\\\]*?(?:\\\\.[^'\\\\]*?)*?'[^']*?)*?" +
				// Optional schema name and dot, group 1 is schema name
				"(?:([a-zA-Z_]\\w*)\\.)?" +
				// Required table name and dot, group 2 is table name. Brackets are MS SQL Server delimiters
				"(\\[?[a-zA-Z_][a-zA-Z_0-9-]*\\]?)\\." +
				// Required column name , is group 3
				"(\\w+)");
	private static final java.util.regex.Pattern attributeRegexLax = 
		java.util.regex.Pattern.compile(
				// Optional schema name and dot, group 1 is schema name
				"(?:([^.]+)\\.)?" +
				// Required table name and dot, group 2 is table name
				"([^.]+)\\." +
				// Required column name, is group 3
				"([^.]+)");
	private static final java.util.regex.Pattern relationNameRegex = 
		java.util.regex.Pattern.compile(
				// Optional schema name and dot, group 1 is schema name
				"(?:([a-zA-Z_]\\w*)\\.)?" +
				// Required table name, group 2 is table name
		"([a-zA-Z_]\\w*)");
	
	/**
	 * Parses a SQL "foo AS bar" expression (for d2rq:alias).
	 */
	public static AliasDeclaration parseAlias(String expression) {
		Matcher matcher = aliasPattern.matcher(expression);
		if (!matcher.matches()) {
			throw new D2RQException("d2rq:alias '" + expression +
					"' is not in 'table AS alias' form", D2RQException.SQL_INVALID_ALIAS);
		}
		return new AliasDeclaration(Microsyntax.parseTable(matcher.group(1)), 
				Microsyntax.parseTable(matcher.group(2)));
	}
	private static final Pattern aliasPattern = 
		Pattern.compile("(.+)\\s+AS\\s+(.+)", Pattern.CASE_INSENSITIVE);

	private Microsyntax() {
		// Can't be instantiated
	}
}
