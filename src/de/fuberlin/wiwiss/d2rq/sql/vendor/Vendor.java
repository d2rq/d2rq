package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;

/**
 * Encapsulates differences in SQL syntax between database engines.
 * Methods only exists for SQL features where at least one engine
 * requires custom syntax differing from SQL-92.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface Vendor {

	public final static Vendor SQL92 = new SQL92(true);
	public final static Vendor MySQL = new MySQL();
	public final static Vendor PostgreSQL = new PostgreSQL();
	public final static Vendor InterbaseOrFirebird = new SQL92(false);
	public final static Vendor Oracle = new Oracle();
	public final static Vendor SQLServer = new SQLServer();
	public final static Vendor MSAccess = new SQLServer(); // TODO
	public final static Vendor HSQLDB = new HSQLDB();

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
	 * Handles special characters in strings. Most databases wrap the string
	 * in single quotes, and escape single quotes by doubling them. Some
	 * databases also require doubling of backslashes.
	 * 
	 * @param s An arbitrary character string
	 * @return A quoted and escaped version safe for use in SQL statements
	 */
	String quoteStringLiteral(String s);
	
	String quoteBinaryLiteral(String hexString);
	
	String quoteDateLiteral(String date);
	
	String quoteTimeLiteral(String time);
	
	String quoteTimestampLiteral(String timestamp);
	
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
	
	/**
	 * Returns a {@link DataType} corresponding to a JDBC type. This may be
	 * an unsupported datatype; in this case, its {@link DataType#isUnsupported()}
	 * method will return true. <code>null</code> will be returned if the vendor
	 * code doesn't handle this datatype at all; that should generally be
	 * considered a bug.
	 * 
	 * @param jdbcType A <code>java.sql.Types</code> constant
	 * @param name The type name, as reported by <code>java.sql</code> metadata methods, normalized to uppercase
	 * @param size Character size of the type, or 0 if not applicable
	 * @return A compatible D2RQ DataType instance, or <code>null</code> if the vendor code is broken
	 */
	DataType getDataType(int jdbcType, String name, int size);

	/**
	 * Turns a BOOLEAN expression into an expression that is guaranteed to
	 * be usable in any place where an expression is allowed.
	 * @param expression A boolean expression
	 * @return A simple expression returning an equivalent value, e.g., INT 0 and 1
	 */
	Expression booleanExpressionToSimpleExpression(Expression expression);
	
	/**
	 * TODO Use the Filter interface for this
	 * @param schema A schema name, or <code>null</code> for the connection's default schema
	 * @param table A table name
	 * @return <code>true</code> if this is a system table that doesn't contain user/application data
	 */
	boolean isIgnoredTable(String schema, String table);
	
	/**
	 * Vendor-specific initialization for a database connection.
	 * 
	 * @param connection
	 */
	void initializeConnection(Connection connection) throws SQLException;

	/**
	 * Vendor-specific code to execute prior to query execution. 
	 * Note: only one query can be "active" at a time per connection
	 * 
	 * @param connection
	 */
	void beforeQuery(Connection connection) throws SQLException;

	/**
	 * Vendor-specific code to execute after query execution.
	 * Note: only one query can be "active" at a time per connection
	 * 
	 * @param connection
	 */
	void afterQuery(Connection connection) throws SQLException;

	/**
	 * Vendor-specific cleanup code to execute prior to statement close. 
	 * Note: only one query can be "active" at a time per connection
	 * 
	 * @param connection
	 */
	void beforeClose(Connection connection) throws SQLException;

	/**
	 * Vendor-specific cleanup code to execute after statement close. 
	 * Note: only one query can be "active" at a time per connection
	 * 
	 * @param connection
	 */
	void afterClose(Connection connection) throws SQLException;

	/**
	 * Vendor-specific cleanup code to execute prior to statement cancel. 
	 * Note: only one query can be "active" at a time per connection
	 * 
	 * @param connection
	 */
	void beforeCancel(Connection connection) throws SQLException;

	/**
	 * Vendor-specific cleanup code to execute after statement cancel. 
	 * Note: only one query can be "active" at a time per connection
	 * 
	 * @param connection
	 */
	void afterCancel(Connection connection) throws SQLException;
}
