package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Properties;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Encapsulates differences in SQL syntax between database engines.
 * Methods only exists for SQL features where at least one engine
 * requires custom syntax differing from SQL-92.
 * 
 * TODO Move all engine-specific code from ConnectedDB to this interface and its implementing classes
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface SQLSyntax {

	/**
	 * Concatenation of <code>a</code> and <code>b</code> is
	 * "<code>a || b</code>" in standard SQL, but <code>CONCAT(a, b)</code>
	 * in MySQL.
	 * 
	 * @param sqlFragments An array of SQL expressions to be concatenated
	 * @return A SQL expression that concatenates the arguments
	 */
	String getConcatenationExpression(String[] sqlFragments);
	
	/**
	 * A relation name with an alias name for use in <code>FROM</code>
	 * clauses. Would return <code>relation AS alias</code> for
	 * SQL 92 (the AS is optional in SQL 92, but some engines require
	 * it, while others don't understand it).
	 *  
	 * @param relationName The original table name
	 * @param aliasName The alias for the table name
	 * @return An expression that assigns the alias to the table name
	 */
	String getRelationNameAliasExpression(RelationName relationName, RelationName aliasName);
	
	/**
	 * Handles special characters in attribute names.
	 * 
	 * @param attribute An attribute name (column name)
	 * @return Quoted form for use in SQL statements
	 */
	String quoteAttribute(Attribute attribute);
	
	/**
	 * Handles special characters in relation names.
	 * 
	 * @param relationName A relation name (table name)
	 * @return Quoted form for use in SQL statements
	 */
	String quoteRelationName(RelationName relationName);

	/**
	 * Handles special characters in identifiers. SQL 92 puts identifiers
	 * in double quotes, but MySQL uses backticks.
	 * 
	 * @param identifier An identifier, such as a table or column name
	 * @return Quoted form of the identifier for use in SQL statements
	 */
	String quoteIdentifier(String identifier);
	
	/**
	 * Returns an expression for limiting the number of returned rows
	 * for engines that support this (<code>ROWNUM &lt;= n</code>)
	 * 
	 * @param limit A maximum number of rows, or {@link Database#NO_LIMIT}
	 * @return An expression that limits the number of rows, or {@link Expression#TRUE}
	 * if not supported by the engine
	 */
	Expression getRowNumLimitAsExpression(int limit);

	/**
	 * Returns a modifier for the SELECT keyword that adds a limit
	 * to the number of returned rows for engines that support this (<code>TOP n</code>)
	 * 
	 * @param limit A maximum number of rows, or {@link Database#NO_LIMIT}
	 * @return A SELECT keyword modifier, or the empty string if unsupported/unnecessary
	 */
	String getRowNumLimitAsSelectModifier(int limit);
	
	/**
	 * Returns a fragment to be appended to a SQL query in order to add a limit
	 * to the number of returned rows for engines that support this (<code>LIMIT n</code>)
	 * 
	 * @param limit A maximum number of rows, or {@link Database#NO_LIMIT}
	 * @return A SQL fragment, or the empty string if unsupported/unnecessary
	 */
	String getRowNumLimitAsQueryAppendage(int limit);
	
	/**
	 * Returns a set of default connection properties to be used
	 * when connecting to this database engine type
	 * 
	 * @return A collection of properties
	 */
	Properties getDefaultConnectionProperties();
}
