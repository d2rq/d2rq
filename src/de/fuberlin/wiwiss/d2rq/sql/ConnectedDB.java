package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Driver;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;
 
/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ConnectedDB.java,v 1.32 2009/08/06 11:15:54 fatorange Exp $
 */
public class ConnectedDB {
	private static final Log log = LogFactory.getLog(ConnectedDB.class);
	
	public static final String MySQL = "MySQL";
	public static final String PostgreSQL = "PostgreSQL";
	public static final String Oracle = "Oracle";
	public static final String MSSQL = "Microsoft SQL Server";
	public static final String MSAccess = "Microsoft Access";
	public static final String Other = "Other";
	public static final int TEXT_COLUMN = 1;
	public static final int NUMERIC_COLUMN = 2;
	public static final int DATE_COLUMN = 3;
	public static final int TIMESTAMP_COLUMN = 4;

	public static final String KEEP_ALIVE_PROPERTY = "keepAlive"; // interval property, value in seconds
	public static final int DEFAULT_KEEP_ALIVE_INTERVAL = 60*60; // hourly
	public static final String KEEP_ALIVE_QUERY_PROPERTY = "keepAliveQuery"; // override default keep alive query
	public static final String DEFAULT_KEEP_ALIVE_QUERY = "SELECT 1"; // may not work for some DBMS
	
	/* 
	 * Backported java.sql types
	 */
	public final static int SQL_TYPE_NVARCHAR = -9;

	private static final String ORACLE_SET_DATE_FORMAT = "ALTER SESSION SET NLS_DATE_FORMAT = 'SYYYY-MM-DD'";
	private static final String ORACLE_SET_TIMESTAMP_FORMAT = "ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'SYYYY-MM-DD HH24:MI:SS'";

	private String jdbcURL;
	private String username;
	private String password;
	private boolean allowDistinct;
	private Set textColumns;
	private Set numericColumns;
	private Set dateColumns;
	private Set timestampColumns;
	private Connection connection = null;
	private DatabaseSchemaInspector schemaInspector = null;
	private String dbType = null;
	private int limit;
	private int fetchSize;
	private Map zerofillCache = new HashMap(); // Attribute => Boolean
	private Map uniqueIndexCache = new HashMap(); // RelationName => String => List of Strings
	private final Properties connectionProperties;

	private class KeepAliveAgent extends Thread {
		private final int interval;
		private final String query;
		volatile boolean shutdown = false;
		
		/**
		 * @param interval in seconds
		 */
		public KeepAliveAgent(int interval, String query) {
			this.interval = interval;
			this.query = query;
		}
		
		public void run() {
			Connection c;
			Statement s = null;
			while (!shutdown) {
				try { Thread.sleep(interval*1000); }
				catch (InterruptedException e) { if (shutdown) break; }
				
				try {
					if (log.isDebugEnabled())
						log.debug("Keep alive agent is executing noop query '" + query + "'...");
					c = connection();
					s = c.createStatement();
					s.execute(query);
					s.close();
				} catch (Throwable e) { // may throw D2RQException at runtime
					log.error("Keep alive connection test failed: " + e.getMessage());
				} finally {
					if (s != null) try { s.close(); } catch (Exception ignore) {}
				}
			}
			
			log.info("Keep alive agent terminated.");
		}
		
		public void shutdown() {
			shutdown = true;
			keepAliveAgent.interrupt();
		}
	};
	
	private final Thread shutdownHook = new Thread() {
		public void run() {
			keepAliveAgent.shutdown();
		}
	};
	
	private final KeepAliveAgent keepAliveAgent;
	
	public ConnectedDB(String jdbcURL, String username, String password) {
		this(jdbcURL, username, password, true,
				Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET, 
				Collections.EMPTY_SET, Database.NO_LIMIT, Database.NO_FETCH_SIZE, null);
	}
	
	public ConnectedDB(String jdbcURL, String username, String password,
			boolean allowDistinct, Set textColumns, Set numericColumns, Set dateColumns,
			Set timestampColumns, int limit, int fetchSize, Properties connectionProperties) {
		this.jdbcURL = jdbcURL;
		this.allowDistinct = allowDistinct;
		this.username = username;
		this.password = password;
		this.textColumns = textColumns;
		this.numericColumns = numericColumns;
		this.dateColumns = dateColumns;
		this.timestampColumns = timestampColumns;
		this.limit = limit;
		this.fetchSize = fetchSize;
		this.connectionProperties = connectionProperties;
		
		// create keep alive agent if enabled
		if (connectionProperties != null && connectionProperties.containsKey(KEEP_ALIVE_PROPERTY)) {
			int interval = DEFAULT_KEEP_ALIVE_INTERVAL;
			String query = DEFAULT_KEEP_ALIVE_QUERY;
			try {
				interval = new Integer((String) connectionProperties.get(KEEP_ALIVE_PROPERTY)).intValue();
				if (interval <= 0) interval = DEFAULT_KEEP_ALIVE_INTERVAL;
			} catch (NumberFormatException ignore) {	} // use default
			if (connectionProperties.containsKey(KEEP_ALIVE_QUERY_PROPERTY))
				query = connectionProperties.getProperty(KEEP_ALIVE_QUERY_PROPERTY);
			
			this.keepAliveAgent = new KeepAliveAgent(interval, query);
			this.keepAliveAgent.start();
			Runtime.getRuntime().addShutdownHook(shutdownHook);
			log.info("Keep alive agent is enabled (interval: " + interval + " seconds, noop query: '" + query + "').");
		} else
			this.keepAliveAgent = null;
	}
	
	public Connection connection() {
		if (this.connection == null) {
			connect();
		}
		return this.connection;
	}
	
	public int limit() {
		return this.limit;
	}
	
	public int fetchSize() {
		return this.fetchSize;
	}

	private void connect() {
		try {
			if (this.jdbcURL.contains(":oracle:")) {
				/* The pre-JSE 6 Oracle driver requires explicit registration */
				try {
					DriverManager.registerDriver((Driver) Class.forName("oracle.jdbc.driver.OracleDriver").getConstructor((Class[])null).newInstance((Object[])null));
				} catch (Exception ex) {
					throw new D2RQException(
							"Unable to register Oracle driver: " + ex.getMessage(), 
							D2RQException.DATABASE_MISSING_JDBCDRIVER);
				}
			}

			this.connection = DriverManager.getConnection(this.jdbcURL, getConnectionProperties());
		} catch (SQLException ex) {
			throw new D2RQException(
					"Database connection to " + jdbcURL + " failed " +
					"(user: " + username + "): " + ex.getMessage(), 
					D2RQException.D2RQ_DB_CONNECTION_FAILED);
		}
		
		/* Database-dependent initialization */
		try {
			/* 
			 * Disable auto-commit in PostgreSQL to support cursors
			 * @see http://jdbc.postgresql.org/documentation/83/query.html
			 */
			if (dbTypeIs(PostgreSQL))
				this.connection.setAutoCommit(false);
						
			/*
			 * Set Oracle date formats 
			 */
			if (dbTypeIs(Oracle)) {
				Statement stmt = this.connection.createStatement();
				try
				{
					stmt.execute(ORACLE_SET_DATE_FORMAT);
					stmt.execute(ORACLE_SET_TIMESTAMP_FORMAT);
				}
				catch (SQLException ex) {
					throw new D2RQException("Unable to set date format: " + ex.getMessage(), D2RQException.D2RQ_SQLEXCEPTION);					
				}
				finally {
					stmt.close();
				}
			}
		} catch (SQLException ex) {
			throw new D2RQException(
					"Database initialization failed: " + ex.getMessage(), 
					D2RQException.D2RQ_DB_CONNECTION_FAILED);
		}
	}
	
	private Properties getConnectionProperties() {
		Properties result = (connectionProperties == null)
				? new Properties()
				: (Properties) connectionProperties.clone();
		if (username != null) {
			result.setProperty("user", username);
		}
		if (password != null) {
			result.setProperty("password", password);
		}
		
		/* 
		 * Enable cursor support in MySQL
		 * Note that the implementation is buggy in early MySQL 5.0 releases such
		 * as 5.0.27, which may lead to incorrect results.
		 * This is placed here as a later change requires a call to setClientInfo, which is only available from Java 6 on
		 */
		if (this.jdbcURL.contains(":mysql:")) {
			result.setProperty("useCursorFetch", "true");
			result.setProperty("useServerPrepStmts", "true"); /* prerequisite */
		}
		
		return result;
	}
	
	public DatabaseSchemaInspector schemaInspector() {
		if (this.schemaInspector == null && this.jdbcURL != null) {
			this.schemaInspector = new DatabaseSchemaInspector(this);
		}
		return this.schemaInspector;
	}

	/**
     * Reports the brand of RDBMS.
     * Will currently report one of these constants:
     * 
     * <ul>
     * <li><tt>ConnectedDB.MySQL</tt></li>
     * <li><tt>ConnectedDB.PostgreSQL</tt></li>
     * <li><tt>ConnectedDB.Oracle</tt></li>
     * <li><tt>ConnectedDB.MSSQL</tt></li>
     * <li><tt>ConnectedDB.Other</tt></li>
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
				} else if (productName.indexOf("microsoft sql server") >= 0) {
					this.dbType = ConnectedDB.MSSQL;
				} else if (productName.indexOf("access") >= 0) {
					this.dbType = ConnectedDB.MSAccess;
				} else {
					this.dbType = ConnectedDB.Other;
				}
			} catch (SQLException ex) {
				throw new D2RQException("Database exception", ex);
			}
		}
		return this.dbType;
	}
	
	/**
	 * Reports the brand of RDBMS.
	 * @return <tt>true</tt> if this database is of the given brand
	 * @see #dbType()
	 */
	public boolean dbTypeIs(String candidateType) {
		return candidateType.equals(dbType());
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
    	if (this.timestampColumns.contains(column.qualifiedName())) {
    		return TIMESTAMP_COLUMN;
    	}
		int type = schemaInspector().columnType(column);
		switch (type) {
			// TODO There are a bunch of others, see http://java.sun.com/j2se/1.5.0/docs/api/java/sql/Types.html
			case Types.CHAR: return TEXT_COLUMN;
			case Types.VARCHAR: return TEXT_COLUMN;
			case ConnectedDB.SQL_TYPE_NVARCHAR: return TEXT_COLUMN;
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
			case Types.BOOLEAN: return NUMERIC_COLUMN;
			
			// TODO: What to do with binary columns?
			case Types.BINARY: return TEXT_COLUMN;
			case Types.VARBINARY: return TEXT_COLUMN;
			case Types.LONGVARBINARY: return TEXT_COLUMN;
			case Types.CLOB: return TEXT_COLUMN;
	
			case Types.DATE: return DATE_COLUMN;
			case Types.TIME: return DATE_COLUMN;
			case Types.TIMESTAMP: return TIMESTAMP_COLUMN;
			
			default: throw new D2RQException("Unsupported database type code (" + type + ") for column "
					+ column.qualifiedName());
		}
	}

	/**
	 * <p>Checks if two columns are formatted by the database in a compatible
	 * fashion.</p>
	 * 
	 * <p>Assuming <tt>v1</tt> is a value from column1, and <tt>v2</tt> a value
	 * from column2, and <tt>v1 = v2</tt> evaluates to <tt>true</tt> within the
	 * database, then we call the values have <em>compatible formatting</em> if
	 * <tt>SELECT</tt>ing them results in character-for-character identical
	 * strings. As an example, a <tt>TINYINT</tt> and a <tt>BIGINT</tt> are
	 * compatible because equal values will be formatted in the same way
	 * when <tt>SELECT</tt>ed, e.g. <tt>1 = 1</tt>. But if one of them is
	 * <tt>ZEROFILL</tt>, then <tt>SELECT</tt>ing will result in a different
	 * character string, e.g. <tt>1 = 0000000001</tt>. The two columns wouldn't
	 * be compatible.</p>
	 * 
	 * <p>This is used by the engine when removing unnecessary joins. If
	 * two columns have compatible formatting, then we can sometimes use
	 * one in place of the other when they are known to have equal values.
	 * But not if they are incompatible, because e.g. <tt>http://example.org/id/1</tt>
	 * is different from <tt>http://example.org/id/0000000001</tt>.</p>
	 * 
	 * @return <tt>true</tt> if both arguments have compatible formatting
	 */
	public boolean areCompatibleFormats(Attribute column1, Attribute column2) {
		// TODO Right now we only catch the ZEROFILL case. There are many more!
		return !isZerofillColumn(column1) && !isZerofillColumn(column2);
	}
	
	private boolean isZerofillColumn(Attribute column) {
		if (!dbTypeIs(MySQL)) return false;
		if (!zerofillCache.containsKey(column)) {
			zerofillCache.put(column, 
					new Boolean(schemaInspector().isZerofillColumn(column)));
		}
		return ((Boolean) zerofillCache.get(column)).booleanValue();
	}
	
	public HashMap getUniqueKeyColumns(RelationName tableName) {
		if (!uniqueIndexCache.containsKey(tableName) && schemaInspector() != null)
			uniqueIndexCache.put(tableName, schemaInspector().uniqueColumns(tableName));
		return (HashMap) uniqueIndexCache.get(tableName);
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

	public String quoteValue(String value, int columnType) {
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
			// TODO: Acces requires "#2006-09-15#"
			return "DATE '" + value + "'";
		} else if (columnType == ConnectedDB.TIMESTAMP_COLUMN) {
			// TODO: Acces requires "#2006-09-15 23:59:00#" (?)
			return "TIMESTAMP '" + value + "'";
		}
		return singleQuote(value);
	}
	
	public String quoteValue(String value, Attribute column) {
	    return quoteValue(value, columnType(column));
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
	
	/** 
	 * Some Databases do not handle large entries correctly.
	 * For example MSAccess cuts strings larger than 256 bytes when queried
	 * with the DISTINCT keyword.
	 * TODO We would need some assertions about a database or specific columns.
	 */
	public boolean allowDistinct() {
		return this.allowDistinct;
	}
	
	/**
	 * In some situations, MySQL stores table names using lowercase only, and then performs
	 * case-insensitive comparison.
	 * We need to account for this when comparing table names reported by MySQL and those from the mapping.   
	 * 
	 * @see <a href="http://dev.mysql.com/doc/refman/5.0/en/identifier-case-sensitivity.html">MySQL Manual, Identifier Case Sensitivity</a>
	 */
	public boolean lowerCaseTableNames() {
		Connection c = connection();
		if (c instanceof com.mysql.jdbc.ConnectionImpl)
			return ((com.mysql.jdbc.ConnectionImpl)c).lowerCaseTableNames();
		else
			return false;
	}

	public void close() {
		if (keepAliveAgent != null)
			keepAliveAgent.shutdown();
		
		if (connection != null) try {
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