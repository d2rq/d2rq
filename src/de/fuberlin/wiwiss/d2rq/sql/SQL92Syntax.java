package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Properties;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * This base class implements SQL-92 compatible syntax. Subclasses
 * can override individual methods to implement different syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQL92Syntax implements SQLSyntax {
	private boolean useAS;
	
	/**
	 * Initializes a new instance.
	 * 
	 * @param useAS Use "Table AS Alias" or "Table Alias" in FROM clauses? In standard SQL, either is fine.
	 */
	public SQL92Syntax(boolean useAS) {
		this.useAS = useAS;
	}
	
	public String getConcatenationExpression(String[] sqlFragments) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < sqlFragments.length; i++) {
			if (i > 0) {
				result.append(" || ");
			}
			result.append(sqlFragments[i]);
		}
		return result.toString();
	}

	public String getRelationNameAliasExpression(RelationName relationName,
			RelationName aliasName) {
		return quoteRelationName(relationName) + (useAS ? " AS " : " ") + quoteRelationName(aliasName);
	}
	
	public String quoteAttribute(Attribute attribute) {
		return quoteRelationName(attribute.relationName()) + "." + 
				quoteIdentifier(attribute.attributeName());
	}
	
	public String quoteRelationName(RelationName relationName) {
		if (relationName.schemaName() == null) {
			return quoteIdentifier(relationName.tableName());
		}
		return quoteIdentifier(relationName.schemaName()) + "." + quoteIdentifier(relationName.tableName());
	}
	
	public String quoteIdentifier(String identifier) {
		return doubleQuote(identifier);
	}
	
	/**
	 * Wraps s in single quotes and escapes special characters to avoid SQL injection
	 */
	protected String doubleQuote(String s) {
		return "\"" + doubleQuoteEscapePattern.matcher(s).
				replaceAll("$1$1") + "\"";
	}
	private final static Pattern doubleQuoteEscapePattern = Pattern.compile("(\")");

	public Expression getRowNumLimitAsExpression(int limit) {
		return Expression.TRUE;
	}

	/**
	 * Technically speaking, SQL 92 supports NO way of limiting
	 * result sets (ROW_NUMBER appeared in SQL 2003). We will
	 * just use MySQL's LIMIT as it appears to be widely implemented.
	 */
	public String getRowNumLimitAsQueryAppendage(int limit) {
		if (limit == Database.NO_LIMIT) return "";
		return "LIMIT " + limit;
	}

	public String getRowNumLimitAsSelectModifier(int limit) {
		return "";
	}

	public Properties getDefaultConnectionProperties() {
		return new Properties();
	}
}
