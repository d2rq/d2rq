package de.fuberlin.wiwiss.d2rq.sql;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.rdql.ConstraintHandler;
import de.fuberlin.wiwiss.d2rq.rdql.ExpressionTranslator;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ConnectedDB.java,v 1.5 2006/09/15 15:31:23 cyganiak Exp $
 */
public class ConnectedDB {
	public static final String MySQL = "MySQL";
	public static final String PostgreSQL = "PostgreSQL";
	public static final String Oracle = "Oracle";
	public static final String Other = "Other";
	public static final int TEXT_COLUMN = 1;
	public static final int NUMERIC_COLUMN = 2;
	public static final int DATE_COLUMN = 3;

	private String jdbcURL;
	private String username;
	private String password;
	private String expressionTranslator;
	private boolean allowDistinct;
	private Set textColumns;
	private Set numericColumns;
	private Set dateColumns;
	private Connection connection = null;
	private DatabaseSchemaInspector schemaInspector = null;
	private String dbType = null;
	
	public ConnectedDB(String jdbcURL, String username, String password) {
		this(jdbcURL, username, password, null, true,
				Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET);
	}
	
	public ConnectedDB(String jdbcURL, String username, String password, String expressionTranslator,
			boolean allowDistinct, Set textColumns, Set numericColumns, Set dateColumns) {
		this.jdbcURL = jdbcURL;
		this.expressionTranslator = expressionTranslator;
		this.allowDistinct = allowDistinct;
		this.username = username;
		this.password = password;
		this.textColumns = textColumns;
		this.numericColumns = numericColumns;
		this.dateColumns = dateColumns;
	}
	
	public Connection connection() {
		if (this.connection == null) {
			this.connection = connect();
		}
		return this.connection;
	}
	
	private Connection connect() {
		try {
			return DriverManager.getConnection(this.jdbcURL, this.username, this.password);
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
	
	public DatabaseSchemaInspector schemaInspector() {
		if (this.schemaInspector == null) {
			this.schemaInspector = new DatabaseSchemaInspector(this);
		}
		return this.schemaInspector;
	}

	/**
     * Reports the brand of RDBMS.
     * Will currently report one of these constants:
     * 
     * <ul>
     * <li><tt>DBConnection.MySQL</tt></li>
     * <li><tt>DBConnection.Other</tt></li>
     * </ul>
     * @return The brand of RDBMS
     */
	public String dbType() {
		if (this.dbType == null) {
			try {
				String productName = connection().getMetaData().getDatabaseProductName().toLowerCase();
				if (productName.indexOf("mysql") >= 0) {
					this.dbType = ConnectedDB.MySQL;
				} else if (productName.indexOf("postgresql") >= 0) {
					this.dbType = ConnectedDB.PostgreSQL;
				} else if (productName.indexOf("oracle") >= 0) {
					this.dbType = ConnectedDB.Oracle;
				} else {
					this.dbType = ConnectedDB.Other;
				}
			} catch (SQLException ex) {
				throw new D2RQException("Database exception", ex);
			}
		}
		return this.dbType;
	}
	
	public boolean dbTypeIs(String candidateType) {
		return candidateType.equals(dbType());
	}
	
    public int columnType(String qualifiedColumnName) {
		return columnType(SQL.parseAttribute(qualifiedColumnName));
    }
    
    /**
     * Returns the columnType for a given database column.
     * @return Node columnType D2RQ.textColumn or D2RQ.numericColumn or D2RQ.dateColumn
     */
    public int columnType(Attribute column) {
    	if (this.textColumns.contains(column.qualifiedName())) {
    		return TEXT_COLUMN;
    	}
    	if (this.numericColumns.contains(column.qualifiedName())) {
    		return NUMERIC_COLUMN;
    	}
    	if (this.dateColumns.contains(column.qualifiedName())) {
    		return DATE_COLUMN;
    	}
		int type = schemaInspector().columnType(column);
		switch (type) {
			// TODO There are a bunch of others, see http://java.sun.com/j2se/1.5.0/docs/api/java/sql/Types.html
			case Types.CHAR: return TEXT_COLUMN;
			case Types.VARCHAR: return TEXT_COLUMN;
			case Types.LONGVARCHAR: return TEXT_COLUMN;
			case Types.NUMERIC: return NUMERIC_COLUMN;
			case Types.DECIMAL: return NUMERIC_COLUMN;
			case Types.BIT: return NUMERIC_COLUMN;
			case Types.TINYINT: return NUMERIC_COLUMN;
			case Types.SMALLINT: return NUMERIC_COLUMN;
			case Types.INTEGER: return NUMERIC_COLUMN;
			case Types.BIGINT: return NUMERIC_COLUMN;
			case Types.REAL: return NUMERIC_COLUMN;
			case Types.FLOAT: return NUMERIC_COLUMN;
			case Types.DOUBLE: return NUMERIC_COLUMN;
	
			// TODO: What to do with binary columns?
			case Types.BINARY: return TEXT_COLUMN;
			case Types.VARBINARY: return TEXT_COLUMN;
			case Types.LONGVARBINARY: return TEXT_COLUMN;
	
			case Types.DATE: return DATE_COLUMN;
			case Types.TIME: return DATE_COLUMN;
			case Types.TIMESTAMP: return DATE_COLUMN;
			
			default: throw new D2RQException("Unsupported database type code (" + type + ") for column "
					+ column.qualifiedName());
		}
	}

	private final static Pattern singleQuoteEscapePattern = Pattern.compile("([\\\\'])");
	private final static Pattern singleQuoteEscapePatternOracle = Pattern.compile("(')");
	private final static Pattern doubleQuoteEscapePattern = Pattern.compile("(\")");
	private final static Pattern backtickEscapePatternMySQL = Pattern.compile("([\\\\`])");
	
	/**
	 * Wraps s in single quotes and escapes special characters to avoid SQL injection
	 */
	public String singleQuote(String s) {
		if (dbTypeIs(Oracle)) {
			return "'" + singleQuoteEscapePatternOracle.matcher(s).
					replaceAll("$1$1") + "'";
		}
		return "'" + singleQuoteEscapePattern.matcher(s).
				replaceAll("$1$1") + "'";
	}

	/**
	 * Wraps s in single quotes and escapes special characters to avoid SQL injection
	 */
	public String doubleQuote(String s) {
		return "\"" + doubleQuoteEscapePattern.matcher(s).
				replaceAll("$1$1") + "\"";
	}

	/**
	 * Wraps s in backticks and escapes special characters to avoid SQL injection
	 */
	public String backtickQuote(String s) {
		return "`" + backtickEscapePatternMySQL.matcher(s).
				replaceAll("$1$1") + "`";
	}

	public String quoteValue(String value, Attribute column) {
	    int columnType = columnType(column);
		if (columnType == ConnectedDB.NUMERIC_COLUMN) {
			// Check if it actually is a number to avoid SQL injection
			try {
				return Integer.toString(Integer.parseInt(value));
			} catch (NumberFormatException nfex) {
				try {
					return Double.toString(Double.parseDouble(value));
				} catch (NumberFormatException nfex2) {
					// No number -- return as quoted string
					// DBs seem to interpret non-number strings as 0
					return singleQuote(value);
				}
			}
		} else if (columnType == ConnectedDB.DATE_COLUMN) {
			// TODO: This works in Access only! Should be:
			// DATE '2006-09-15'
			// TIMESTAMP '2006-09-15 12:34:56.7890'
			// depending on type of column. This is SQL-92 and confirmed to 
			// work in MySQL, PostgreSQL, Oracle.
			return "#" + value + "#";
		}
		return singleQuote(value);
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
	
	private String quoteIdentifier(String identifier) {
		// MySQL uses backticks
		if (dbTypeIs(ConnectedDB.MySQL)) {
			return backtickQuote(identifier);
		}
		// PostgreSQL and Oracle (and SQL-92) use double quotes
		return doubleQuote(identifier);
	}

	// TODO Better if we didn't have to pass the parameters in, and use a non-arg constructor
	public ExpressionTranslator expressionTranslator(ConstraintHandler handler) {
		if (this.expressionTranslator == null) {
			return new ExpressionTranslator(handler, this);
		} else {
			try {
				Class c = Class.forName(this.expressionTranslator);
				Constructor constructor = c.getConstructor(
						new Class[]{ConstraintHandler.class});
				return (ExpressionTranslator) constructor.newInstance(new Object[]{handler, this});
		    } catch (ClassNotFoundException ex) {
		    	throw new D2RQException("Couldn't find d2rq:expressionTranslator class " + this.expressionTranslator);
		    } catch (Exception ex) {
		    	throw new D2RQException("Couldn't instantiate " + this.expressionTranslator, ex);
		    }
		}
	}
	
	/** 
	 * Some Databases do not handle large entries correctly.
	 * For example MSAccess cuts strings larger than 256 bytes when queried
	 * with the DISTINCT keyword.
	 * TODO We would need some assertions about a database or specific columns.
	 */
	public boolean allowDistinct() {
		return this.allowDistinct;
	}

	public void close() {
		if (this.connection == null) {
			return;
		}
		try {
			this.connection.close();
		} catch (SQLException ex) {
			throw new D2RQException(ex);
		}
	}
	
    public boolean equals(Object otherObject) {
    	if (!(otherObject instanceof ConnectedDB)) {
    		return false;
    	}
    	ConnectedDB other = (ConnectedDB) otherObject;
    	return this.jdbcURL.equals(other.jdbcURL);
    }
    
    public int hashCode() {
    	return this.jdbcURL.hashCode();
    }
}