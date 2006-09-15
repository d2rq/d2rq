package de.fuberlin.wiwiss.d2rq.sql;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;

/**
 * Parses different types of SQL fragments from Strings, and turns them
 * back into Strings. All methods are static.
 * 
 * TODO: find/rename/quoteColumnsInExpression will fail e.g. for coumn names
 *       occuring inside string literals
 * TODO: Make sure these are only called in the parsing/compilation steps 
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SQL.java,v 1.1 2006/09/15 15:31:22 cyganiak Exp $
 */
public class SQL {
	private static final java.util.regex.Pattern attributeRegex = 
		java.util.regex.Pattern.compile(
				// Optional schema name and dot, group 1 is schema name
				"(?:([a-zA-Z_]\\w*)\\.)?" +
				// Required table name and dot, group 2 is table name
				"([a-zA-Z_]\\w*)\\." +
				// Required column name, is group 3
				"([a-zA-Z_]\\w*)");

	/**
	 * Constructs an attribute from a fully qualified column name in <tt>[schema.]table.column</tt>
	 * notation.
	 * 
	 * @param qualifiedName The attribute's name
	 */
	public static Attribute parseAttribute(String qualifiedName) {
		Matcher match = attributeRegex.matcher(qualifiedName);
		if (!match.matches()) {
			throw new D2RQException("Attribute \"" + qualifiedName + 
					"\" is not in \"[schema.]table.column\" notation",
					D2RQException.SQL_INVALID_ATTRIBUTENAME);
		}
		return new Attribute(match.group(1), match.group(2), match.group(3));
	}

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
	
	// TODO: This really should happen in Expression and without string munging
	public static String quoteColumnsInExpression(String expression, ConnectedDB database) {
		StringBuffer result = new StringBuffer();
		Matcher match = attributeRegex.matcher(expression);
		boolean matched = match.find();
		int firstPartEnd = matched ? match.start() : expression.length();
		result.append(expression.substring(0, firstPartEnd));
		while (matched) {
			result.append(database.quoteAttribute(
					new Attribute(match.group(1), match.group(2), match.group(3))));
			int nextPartStart = match.end();
			matched = match.find();
			int nextPartEnd = matched ? match.start() : expression.length();
			result.append(expression.substring(nextPartStart, nextPartEnd));
		}
		return result.toString();
	}

	private static final java.util.regex.Pattern relationNameRegex = 
		java.util.regex.Pattern.compile(
				// Optional schema name and dot, group 1 is schema name
				"(?:([a-zA-Z_]\\w*)\\.)?" +
				// Required table name, group 2 is table name
		"([a-zA-Z_]\\w*)");

	/**
	 * Constructs a relation name from a fully qualified name in <tt>schema.table</tt>
	 * or <tt>table</tt> notation.
	 * 
	 * @param qualifiedName The relation's name
	 */
	public static RelationName parseRelationName(String qualifiedName) {
		Matcher match = relationNameRegex.matcher(qualifiedName);
		if (!match.matches()) {
			throw new D2RQException("Relation name \"" + qualifiedName + 
					"\" is not in \"[schema.]table\" notation",
					D2RQException.SQL_INVALID_RELATIONNAME);
		}
		return new RelationName(match.group(1), match.group(2));
	}

	private static final Pattern aliasPattern = 
		Pattern.compile("(.+)\\s+AS\\s+(.+)", Pattern.CASE_INSENSITIVE);

	/**
	 * Constructs an Alias from an SQL "foo AS bar" expression.
	 */
	public static Alias parseAlias(String aliasExpression) {
		Matcher matcher = aliasPattern.matcher(aliasExpression);
		if (!matcher.matches()) {
			throw new D2RQException("d2rq:alias '" + aliasExpression +
					"' is not in 'table AS alias' form", D2RQException.SQL_INVALID_ALIAS);
		}
		return new Alias(SQL.parseRelationName(matcher.group(1)), 
				SQL.parseRelationName(matcher.group(2)));
	}
	
	private SQL() {
		// Can't be instantiated
	}
}
