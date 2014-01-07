package org.d2rq.db.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Identifier.IdentifierParseException;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.lang.Database;


/**
 * Encapsulates differences in SQL syntax between database engines.
 * Methods only exists for SQL features where at least one engine
 * requires custom syntax differing from SQL-92.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface Vendor {
	public final static Log log = LogFactory.getLog(Vendor.class);
	
	public final static Vendor SQL92 = new SQL92(true);
	public final static Vendor MySQL = new MySQL();
	public final static Vendor PostgreSQL = new PostgreSQL();
	public final static Vendor InterbaseOrFirebird = new InterbaseOrFirebird();
	public final static Vendor Oracle = new Oracle();
	public final static Vendor SQLServer = new SQLServer();
	public final static Vendor MSAccess = SQLServer; // TODO
	public final static Vendor Sybase = SQLServer; // TODO -- they are *mostly* identical
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
	 * An alias declaration for use in <code>FROM</code> clauses.
	 * Would return <code> AS </code> for SQL 92, and a single space
	 * for Oracle (the AS is optional in SQL 92, but some engines require
	 * it, while others don't understand it).
	 *  
	 * @return An operator for assigning an alias to a table specification
	 */
	String getAliasOperator();
	
	/**
	 * For databases that support SQL queries without a <code>FROM</code>
	 * clause, (<code>SELECT 1+1</code>), this should return <code>null</code>.
	 * For databases that require some sort of dummy table (e.g., Oracle:
	 * <code>SELECT 1+1 FROM DUAL</code>), this should return the name of
	 * that table.
	 */
	String getTrueTable();
	
	/**
	 * Parses SQL identifiers, such as column names and table names. These
	 * might be qualified, that is, multiple period-separated identifiers.
	 * They might also be quoted if this is supported by the SQL syntax.
	 * For example, a fully qualified column name might be:
	 * <code>MYCATALOG.MYSCHEMA."Table 1".ID</code>
	 * 
	 * @param s The input string
	 * @param minParts Minimum required identifiers, e.g., 1 for an unqualified identifier
	 * @param maxParts Maximum required identifiers, e.g., 4 for a fully qualified column name
	 * @return Array of identifiers
	 * @throws IdentifierParseException on invalid syntax
	 */
	Identifier[] parseIdentifiers(String s, int minParts, int maxParts)
	throws IdentifierParseException;
	
	/**
	 * Handles special characters in identifiers. For example, SQL 92 puts
	 * delimited identifiers in double quotes, but MySQL uses backticks.
	 * 
	 * @param identifier An identifier, such as an unqualified table or column name
	 * @return A string representation of the identifier for use in SQL statements
	 */
	String toString(Identifier identifier);
	
	/**
	 * Handles special characters in qualified table names.
	 * 
	 * @param table A qualified table name
	 * @return A string representation of the table name for use in SQL statements
	 */
	String toString(TableName table);
	
	/**
	 * Handles special characters in qualified column names.
	 * 
	 * @param column A qualified column name
	 * @return A string representation of the column name for use in SQL statements
	 */
	String toString(ColumnName column);
	
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
	 * 
	 * @param catalog A catalog name, or <code>null</code> for the connection's default schema
	 * @param schema A schema name, or <code>null</code> for the connection's default schema
	 * @param table A table name
	 * @return <code>true</code> if this is a system table that doesn't contain user/application data
	 */
	boolean isIgnoredTable(String catalog, String schema, String table);
	
	/**
	 * Returns a qualified table name from catalog/schema/table strings as
	 * reported from JDBC metadata. Vendor implementations can override this
	 * to rename schemas, mess around with case sensitivity, etc.
	 * 
	 * @param catalog A catalog name, or <code>null</code> for the connection's default schema
	 * @param schema A schema name, or <code>null</code> for the connection's default schema
	 * @param table A table name
	 * @return A QualfiedTableName instance that names the table
	 */
	TableName toQualifiedTableName(String catalog, String schema, String table);

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
