package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.Quoter;
import de.fuberlin.wiwiss.d2rq.sql.Quoter.PatternDoublingQuoter;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLApproximateNumeric;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBinary;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBit;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBoolean;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLCharacterString;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLDate;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLExactNumeric;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLTime;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLTimestamp;
import de.fuberlin.wiwiss.d2rq.sql.types.UnsupportedDataType;

/**
 * This base class implements SQL-92 compatible syntax. Subclasses
 * can override individual methods to implement different syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQL92 implements Vendor {
	private boolean useAS;
	
	/**
	 * Initializes a new instance.
	 * 
	 * @param useAS Use "Table AS Alias" or "Table Alias" in FROM clauses? In standard SQL, either is fine.
	 */
	public SQL92(boolean useAS) {
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
		return doubleQuoteEscaper.quote(identifier);
	}
	private final static Quoter doubleQuoteEscaper = 
		new PatternDoublingQuoter(Pattern.compile("(\")"), "\"");

	public String quoteStringLiteral(String s) {
		return singleQuoteEscaper.quote(s);
	}
	private final static Quoter singleQuoteEscaper = 
		new PatternDoublingQuoter(Pattern.compile("(')"), "'");

	public String quoteBinaryLiteral(String hexString) {
		return "X" + quoteStringLiteral(hexString);
	}
	
	public String quoteDateLiteral(String date) {
		return "DATE " + quoteStringLiteral(date);
	}
	
	public String quoteTimeLiteral(String time) {
		return "TIME " + quoteStringLiteral(time);
	}
	
	public String quoteTimestampLiteral(String timestamp) {
		return "TIMESTAMP " + quoteStringLiteral(timestamp);
	}

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
	
	public DataType getDataType(int jdbcType, String name, int size) {
		// TODO: These are in java.sql.Types as of Java 6 but not yet in Java 1.5
		if ("NCHAR".equals(name) || "NVARCHAR".equals(name) || "NCLOB".equals(name)) {
			return new SQLCharacterString(this, name, true);
		}

		
		switch (jdbcType) {
		case Types.CHAR:
		case Types.VARCHAR:
		case Types.LONGVARCHAR:
		case Types.CLOB:
			return new SQLCharacterString(this, name, true);
			
		case Types.BOOLEAN:
			return new SQLBoolean(this, name);

		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			return new SQLBinary(this, name, true);
			
		case Types.BIT:
			return new SQLBit(this, name);

		case Types.NUMERIC:
		case Types.DECIMAL:
		case Types.TINYINT:
		case Types.SMALLINT:
		case Types.INTEGER:
		case Types.BIGINT:
			return new SQLExactNumeric(this, name, jdbcType, false);
			
		case Types.REAL:
		case Types.FLOAT:
		case Types.DOUBLE:
			return new SQLApproximateNumeric(this, name);
		
		case Types.DATE:
			return new SQLDate(this, name);
			
		case Types.TIME:
			return new SQLTime(this, name);
			
		case Types.TIMESTAMP:
			return new SQLTimestamp(this, name);

		case Types.ARRAY:
		case Types.JAVA_OBJECT:
			return new UnsupportedDataType(jdbcType, name);
			
		// TODO: What about the remaining java.sql.Types?
		case Types.DATALINK:
		case Types.DISTINCT:
		case Types.NULL:
		case Types.OTHER:
		case Types.REF:
		}
		
		return null;
	}

	/**
	 * In most databases, we don't have to do anything because boolean
	 * expressions are allowed anywhere.
	 */
	public Expression booleanExpressionToSimpleExpression(Expression expression) {
		return expression;
	}
	
	public boolean isIgnoredTable(String schema, String table) {
		return false;
	}

	public void initializeConnection(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void beforeQuery(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}
	
	public void afterQuery(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void beforeClose(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}
	
	public void afterClose(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void beforeCancel(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}
	
	public void afterCancel(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}
}

