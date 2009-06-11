package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;

/**
 * Parses different types of SQL fragments from Strings, and turns them
 * back into Strings. All methods are static.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SQL.java,v 1.8 2009/06/11 10:57:50 fatorange Exp $
 */
public class SQL {
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

	/**
	 * Constructs an attribute from a fully qualified column name in <tt>[schema.]table.column</tt>
	 * notation.
	 * 
	 * @param qualifiedName The attribute's name
	 */
	public static Attribute parseAttribute(String qualifiedName) {
		Matcher match = attributeRegexLax.matcher(qualifiedName);
		if (!match.matches()) {
			throw new D2RQException("Attribute \"" + qualifiedName + 
					"\" is not in \"[schema.]table.column\" notation",
					D2RQException.SQL_INVALID_ATTRIBUTENAME);
		}
		return new Attribute(match.group(1), match.group(2), match.group(3));
	}

	public static Set findColumnsInExpression(String expression) {
		Set results = new HashSet();
		Matcher match = attributeRegexConservative.matcher(expression);
		while (match.find()) {
			results.add(new Attribute(match.group(1), match.group(2), match.group(3)));
		}
		return results;
	}
	
	public static String replaceColumnsInExpression(String expression, ColumnRenamer columnRenamer) {
		StringBuffer result = new StringBuffer();
		Matcher match = attributeRegexConservative.matcher(expression);
		boolean matched = match.find();
		int firstPartEnd = matched ? (match.start(1) != -1 ? match.start(1)
														   : match.start(2))
								   : expression.length();
		result.append(expression.substring(0, firstPartEnd));
		while (matched) {
			Attribute column = new Attribute(match.group(1), match.group(2), match.group(3));
			result.append(columnRenamer.applyTo(column).qualifiedName());
			int nextPartStart = match.end();
			matched = match.find();
			int nextPartEnd = matched ? (match.start(1) != -1 ? match.start(1)
															  : match.start(2))
									  : expression.length();
			result.append(expression.substring(nextPartStart, nextPartEnd));
		}
		return result.toString();
	}
	
	// TODO: This really should happen in Expression and without string munging
	public static String quoteColumnsInExpression(String expression, ConnectedDB database) {
		StringBuffer result = new StringBuffer();
		Matcher match = attributeRegexConservative.matcher(expression);
		boolean matched = match.find();
		int firstPartEnd = matched ? (match.start(1) != -1 ? match.start(1)
				   				    					   : match.start(2))
				   				   : expression.length();
		result.append(expression.substring(0, firstPartEnd));
		while (matched) {
			result.append(database.quoteAttribute(
					new Attribute(match.group(1), match.group(2), match.group(3))));
			int nextPartStart = match.end();
			matched = match.find();
			int nextPartEnd = matched ? (match.start(1) != -1 ? match.start(1)
					  										  : match.start(2))
					  				  : expression.length();
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
	
	/**
	 * Builds a list of Join objects from a list of join condition
	 * strings. Groups multiple condition that connect the same
	 * two table (multi-column keys) into a single join.
	 * @param joinConditions a collection of strings
	 * @return a set of {@link Join} instances
	 */
	public static Set parseJoins(Collection joinConditions) {
		List parsedConditions = new ArrayList();
		Iterator it = joinConditions.iterator();
		while (it.hasNext()) {
			Attribute[] split = parseJoinCondition((String) it.next());
			parsedConditions.add(new AttributeEqualityCondition(split[0], split[1]));
		}
		Collections.sort(parsedConditions);
		Set results = new HashSet();
		List attributes1 = new ArrayList();
		List attributes2 = new ArrayList();
		AttributeEqualityCondition previousCondition = null;
		it = parsedConditions.iterator();
		while (it.hasNext()) {
			AttributeEqualityCondition condition = (AttributeEqualityCondition) it.next();
			if (previousCondition == null || !condition.sameRelations(previousCondition)) {
				if (previousCondition != null) {
					results.add(new Join(attributes1, attributes2));
				}
				attributes1 = new ArrayList();
				attributes2 = new ArrayList();
			}
			attributes1.add(condition.firstAttribute());
			attributes2.add(condition.secondAttribute());
			previousCondition = condition;
		}
		if (previousCondition != null) {
			results.add(new Join(attributes1, attributes2));
		}
		return results;
	}

	private static Attribute[] parseJoinCondition(String joinCondition) {
		int index = joinCondition.indexOf("=");
		if (index == -1) {
			throw new D2RQException("d2rq:join \"" + joinCondition +
					"\" is not in \"table1.col1 = table2.col2\" form",
					D2RQException.SQL_INVALID_JOIN);
		}
		return new Attribute[]{
				SQL.parseAttribute(joinCondition.substring(0, index).trim()),
				SQL.parseAttribute(joinCondition.substring(index + 1).trim())};
	}
	
	private static class AttributeEqualityCondition implements Comparable {
		private Attribute firstAttribute;
		private Attribute secondAttribute;
		AttributeEqualityCondition(Attribute a1, Attribute a2) {
			this.firstAttribute = (a1.compareTo(a2) < 0) ? a1 : a2;
			this.secondAttribute = (a1.compareTo(a2) < 0) ? a2 : a1;
		}
		public Attribute firstAttribute() { return this.firstAttribute; }
		public Attribute secondAttribute() { return this.secondAttribute; }
		boolean sameRelations(AttributeEqualityCondition otherCondition) {
			return otherCondition.firstAttribute().relationName().equals(firstAttribute().relationName())
					&& otherCondition.secondAttribute().relationName().equals(secondAttribute().relationName());
		}
		public int compareTo(Object otherObject) {
			if (!(otherObject instanceof AttributeEqualityCondition)) return 0;
			AttributeEqualityCondition otherCondition = (AttributeEqualityCondition) otherObject;
			return this.firstAttribute.compareTo(otherCondition.firstAttribute);
		}
	}

	private SQL() {
		// Can't be instantiated
	}
}
