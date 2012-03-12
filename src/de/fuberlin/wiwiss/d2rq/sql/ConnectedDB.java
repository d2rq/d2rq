package de.fuberlin.wiwiss.d2rq.sql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.ColumnType;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;
 
/**
 * TODO Move all engine-specific code from ConnectedDB to this interface and its implementing classes
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author kurtjx (http://github.com/kurtjx)
 */
public class ConnectedDB {
	private static final Log log = LogFactory.getLog(ConnectedDB.class);
	
	public static final String MySQL = "MySQL";
	public static final String PostgreSQL = "PostgreSQL";
	public static final String Oracle = "Oracle";
	public static final String MSSQL = "Microsoft SQL Server";
	public static final String MSAccess = "Microsoft Access";
	public static final String HSQLDB = "HSQLDB";
	public static final String InterbaseOrFirebird = "Interbase/Firebird";
	public static final String Other = "Other";
	
	public static final String KEEP_ALIVE_PROPERTY = "keepAlive"; // interval property, value in seconds
	public static final int DEFAULT_KEEP_ALIVE_INTERVAL = 60*60; // hourly
	public static final String KEEP_ALIVE_QUERY_PROPERTY = "keepAliveQuery"; // override default keep alive query
	public static final String DEFAULT_KEEP_ALIVE_QUERY = "SELECT 1"; // may not work for some DBMS
	
	private static final String ORACLE_SET_DATE_FORMAT = "ALTER SESSION SET NLS_DATE_FORMAT = 'SYYYY-MM-DD'";
	private static final String ORACLE_SET_TIMESTAMP_FORMAT = "ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'SYYYY-MM-DD HH24:MI:SS'";
	
	/*
	 * Definitions of ignored schemas
	 */
	private static final String[] POSTGRESQL_IGNORED_SCHEMAS = {"information_schema", "pg_catalog"};
	private static final String[] ORACLE_IGNORED_SCHEMAS = {"CTXSYS", "EXFSYS", "FLOWS_030000", "MDSYS", "OLAPSYS", "ORDSYS", "SYS", "SYSTEM", "WKSYS", "WK_TEST", "WMSYS", "XDB"};
    private static final List<String> MSSQL_IGNORED_SCHEMAS = Arrays.asList(new String[]{"sys", "INFORMATION_SCHEMA"});
	
	private String jdbcURL;
	private String username;
	private String password;
	private boolean allowDistinct;
	private final Map<String,SQLDataType> columnTypes = new HashMap<String,SQLDataType>();
	private Connection connection = null;
	private DatabaseSchemaInspector schemaInspector = null;
	
	// Lazy initialization for these two -- use the getSyntax() and dbType() for access!
	private String dbType = null;
	private SQLSyntax syntax = null;
	
	private int limit;
	private int fetchSize;
	private Map<Attribute,Boolean> zerofillCache = new HashMap<Attribute,Boolean>();
	private Map<RelationName,Map<String,List<String>>> uniqueIndexCache = 
		new HashMap<RelationName,Map<String,List<String>>>();
	private final Properties connectionProperties;

	private class KeepAliveAgent extends Thread {
		private final int interval;
		private final String query;
		volatile boolean shutdown = false;
		
		/**
		 * @param interval in seconds
		 * @param query the noop query to execute
		 */
		public KeepAliveAgent(int interval, String query) {
			super("keepalive");
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
			
			log.debug("Keep alive agent terminated.");
		}
		
		public void shutdown() {
			log.debug("shutting down");
			shutdown = true;
			this.interrupt();
		}
	};
	
	private final KeepAliveAgent keepAliveAgent;
	
	public ConnectedDB(String jdbcURL, String username, String password) {
		this(jdbcURL, username, password, true,
				Collections.<String,SQLDataType>emptyMap(),
				Database.NO_LIMIT, Database.NO_FETCH_SIZE, null);
	}
	
	public ConnectedDB(String jdbcURL, String username, 
			String password, boolean allowDistinct, 
			Map<String,SQLDataType> columnTypes,
			int limit, int fetchSize, Properties connectionProperties) {
		// TODO replace column type arguments with a single column => type map
		this.jdbcURL = jdbcURL;
		this.allowDistinct = allowDistinct;
		this.username = username;
		this.password = password;
		this.columnTypes.putAll(columnTypes);
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
			log.debug("Keep alive agent is enabled (interval: " + interval + " seconds, noop query: '" + query + "').");
		} else
			this.keepAliveAgent = null;
	}
	
	public String getJdbcURL() {
		return jdbcURL;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
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
			
			if (dbTypeIs(HSQLDB)) {
				// Enable storage of special Double values: NaN, INF, -INF
				Statement stmt = this.connection.createStatement();
				try {
					stmt.execute("SET DATABASE SQL DOUBLE NAN FALSE");
				} catch (SQLException ex) {
					throw new D2RQException("Unable to SET DATABASE SQL DOUBLE NAN FALSE: " + ex.getMessage(), D2RQException.D2RQ_SQLEXCEPTION);
				} finally {
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
	private String dbType() {
		ensureDatabaseTypeInitialized();
		return this.dbType;
	}
	
	/**
	 * Reports the brand of RDBMS.
	 * @return <tt>true</tt> if this database is of the given brand
	 * @see #dbType()
	 * 
	 * TODO make private, use {@link #getSyntax()} and its methods instead
	 */
	public boolean dbTypeIs(String candidateType) {
		return candidateType.equals(dbType());
	}
	
	/**
	 * @return A helper for generating SQL statements conforming to the syntax
	 * of the database engine used in this connection
	 */
	public SQLSyntax getSyntax() {
		ensureDatabaseTypeInitialized();
		return syntax;
	}

	protected String getDatabaseProductType() throws SQLException {
		return connection().getMetaData().getDatabaseProductName();
	}
	
	private void ensureDatabaseTypeInitialized() {
		if (this.dbType != null) return;
		try {
			String productName = getDatabaseProductType().toLowerCase();
			if (productName.indexOf("mysql") >= 0) {
				this.dbType = ConnectedDB.MySQL;
				this.syntax = new MySQLSyntax();
			} else if (productName.indexOf("postgresql") >= 0) {
				this.dbType = ConnectedDB.PostgreSQL;
				this.syntax = new SQL92Syntax(true);
			} else if (productName.indexOf("interbase") >= 0) {
				this.dbType = ConnectedDB.InterbaseOrFirebird;
				this.syntax = new SQL92Syntax(false);
			} else if (productName.indexOf("oracle") >= 0) {
				this.dbType = ConnectedDB.Oracle;
				this.syntax = new OracleSyntax();
			} else if (productName.indexOf("microsoft sql server") >= 0) {
				this.dbType = ConnectedDB.MSSQL;
				this.syntax = new MSSQLSyntax();
			} else if (productName.indexOf("access") >= 0) {
				this.dbType = ConnectedDB.MSAccess;
				this.syntax = new MSSQLSyntax();
			} else if (productName.indexOf("hsql") >= 0) {
				this.dbType = ConnectedDB.HSQLDB;
				this.syntax = new SQL92Syntax(true);
			} else {
				this.dbType = ConnectedDB.Other;
				this.syntax = new SQL92Syntax(true);
			}
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
    /**
     * Returns the data type for a given database column.
     */
    public SQLDataType columnType(Attribute column) {
    	if (columnTypes.containsKey(column.qualifiedName())) {
    		return columnTypes.get(column.qualifiedName());
    	}
		ColumnType type = schemaInspector().columnType(column);
		if (type.typeId() == Types.OTHER && dbTypeIs(HSQLDB)) {
			// OTHER in HSQLDB 2.2.8 is really JAVA_OBJECT
			return SQLDataType.UNMAPPABLE;
		}
		if (type.typeId() == Types.VARCHAR && dbTypeIs(HSQLDB)) {
			// HSQLDB 2.2.8 reports INTERVAL types as VARCHAR 
			if (type.typeName().startsWith("INTERVAL")) {
				return SQLDataType.INTERVAL;
			}
		}
		
		// HACK: MS SQLServer 2008 returns 'date' as VARCHAR type
		if (type.typeName().equals("date") && dbTypeIs(ConnectedDB.MSSQL)) {
			return SQLDataType.DATE;
		}
		
// HACK: MS SQLServer 2008 returns 'datetime2(7)' and 'datetimeoffset(7)' as VARCHAR type
// TODO: Cant make it work. See comment in ResultRowMap.java for additional information on datatype 
// inconsistency particularly in the case of MS SQLServer.
//		if((type.typeName().equals("datetime2") && dbTypeIs(ConnectedDB.MSSQL)) || (type.typeName().equals("datetimeoffset") && dbTypeIs(ConnectedDB.MSSQL))) {
//			return SQLDataType.TIMESTAMP;
//		}

		if (dbTypeIs(ConnectedDB.MySQL) && type.typeId() == Types.BIT
				&& type.size() == 0) {
			// MySQL reports TINYINT(1) as BIT, but all other BITs as BIT(M).
			// This is conventionally treated as BOOLEAN.
			return SQLDataType.BOOLEAN;
		}

		switch (type.typeId()) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.CLOB:
				return SQLDataType.CHARACTER;
			
			case Types.NUMERIC:
			case Types.DECIMAL:
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.REAL:
			case Types.FLOAT:
			case Types.DOUBLE:
				return SQLDataType.NUMERIC;

			case Types.BOOLEAN:
				return SQLDataType.BOOLEAN;

			// TODO: What's this exactly?
			case Types.ROWID:
				return SQLDataType.NUMERIC;

			case Types.BIT:
				return SQLDataType.BIT;

			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.BLOB:
				return SQLDataType.BINARY;

			case Types.DATE: return SQLDataType.DATE;
			case Types.TIME: return SQLDataType.TIME;
			case Types.TIMESTAMP: return SQLDataType.TIMESTAMP;

			case Types.ARRAY:
			case Types.JAVA_OBJECT:
				return SQLDataType.UNMAPPABLE;
			
			// The rest of the types defined in java.sql.Types,
			// we have not worked out what to do with them
			case Types.OTHER:
			case Types.DATALINK:
			case Types.DISTINCT:
			case Types.NULL:
			case Types.REF:
			case Types.STRUCT:
		}
		if ("NCHAR".equals(type.typeName()) || "NVARCHAR".equals(type.typeName()) ||
				"NCLOB".equals(type.typeName())) {
			// These are in java.sql.Types as of Java 6 but not yet in Java 1.5
			return SQLDataType.CHARACTER;
		}
		if ("VARCHAR2".equals(type.typeName()) || "NVARCHAR2".equals(type.typeName())) {
			// Oracle-specific types
			return SQLDataType.CHARACTER;
		}
    	if ("BINARY_FLOAT".equals(type.typeName()) || "BINARY_DOUBLE".equals(type.typeName())) {
    		return SQLDataType.NUMERIC;
    	}
    	if ("BFILE".equals(type.typeName())) {
    		// TODO: We could at least support reading from BFILE, although querying for them seems hard
    		return SQLDataType.UNMAPPABLE;
    	}
		if ("uuid".equals(type.typeName())) {
			// PostgreSQL
			return SQLDataType.CHARACTER;
		}
		if (type.typeName().startsWith("TIMESTAMP")) {
			// Some driver doesn't handle TIMESTAMP property; Oracle???
			// Seen TIMESTAMP(0), TIMESTAMP(6), TIMESTAMP(9)
			return SQLDataType.TIMESTAMP;
		}
		throw new D2RQException("Unsupported database type code (" +
			type.typeId() + ") or type name ('" + type.typeName() +
			"') for column " + column.qualifiedName(), D2RQException.DATATYPE_UNKNOWN);
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
			zerofillCache.put(column, schemaInspector().isZerofillColumn(column));
		}
		return zerofillCache.get(column);
	}
	
	public Map<String,List<String>> getUniqueKeyColumns(RelationName tableName) {
		if (!uniqueIndexCache.containsKey(tableName) && schemaInspector() != null)
			uniqueIndexCache.put(tableName, schemaInspector().uniqueColumns(tableName));
		return uniqueIndexCache.get(tableName);
	}
    
	private final static Pattern singleQuoteEscapePattern = Pattern.compile("([\\\\'])");
	private final static Pattern singleQuoteEscapePatternOracle = Pattern.compile("(')");
	
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

	public final static Pattern DATE_PATTERN = 
		Pattern.compile("^\\d?\\d?\\d?\\d-\\d\\d-\\d\\d$");
	public final static Pattern TIME_PATTERN = 
			Pattern.compile("^\\d?\\d:\\d\\d:\\d\\d(.\\d+)?([+-]\\d?\\d:\\d\\d|Z)?$");
	public final static Pattern TIMESTAMP_PATTERN = 
			Pattern.compile("^\\d?\\d?\\d?\\d-\\d\\d-\\d\\d \\d?\\d:\\d\\d:\\d\\d(.\\d+)?([+-]\\d?\\d:\\d\\d|Z)?$");
	public final static Pattern BINARY_PATTERN = 
		Pattern.compile("^([0-9a-fA-F][0-9a-fA-F])*$");
	
	/**
	 * Creates a SQL literal for the given value, suitable
	 * for comparison to a column of the indicated type.
	 * If the value is not suitable for the column type
	 * (e.g., not a number for a NUMERIC_COLUMN), <code>NULL</code>
	 * is returned.
	 * 
	 * TODO Refactor into {@link SQLDataType}
	 * 
	 * @param value A value
	 * @param columnType Type for which to format the value
	 * @return A quoted and escaped SQL literal, suitable for comparison to a column 
	 */
	public String quoteValue(String value, SQLDataType columnType) {
		if (columnType == SQLDataType.UNMAPPABLE) {
			throw new D2RQException(
					"Attempted to create SQL literal for unmappable datatype",
					D2RQException.DATATYPE_UNMAPPABLE);
		}
		if (value == null) {
			return "NULL";
		}
		if (columnType == SQLDataType.INTERVAL) {
			// TODO: Generate appropriate INTERVAL literal 
			return "NULL";
		}
		if (columnType == SQLDataType.NUMERIC) {
			if (dbTypeIs(HSQLDB)) {
				if ("NaN".equals(value)) {
					return "(0E0/0E0)";
				} else if ("INF".equals(value)) {
					return "(1E0/0)";
				} else if ("-INF".equals(value)) {
					return "(-1E0/0)";
				}
			}
			// Check if it actually is a number to avoid SQL injection
			try {
				return new BigDecimal(value).toString();
			} catch (NumberFormatException nfex) {
				// Scientific notation? E.g., 1E-3
				try {
					double d = Double.parseDouble(value);
					if (Double.isNaN(d) || Double.isInfinite(d)) {
						// Valid in xsd:double, but not supported by vanilla DBs
						return "NULL";
					}
					return Double.toString(d);
				} catch (NumberFormatException nfex2) {
					// Not a number AFAICT
					return "NULL";
				}
			}
		} else if (columnType == SQLDataType.BOOLEAN) {
			if (value == "true" || value == "1") {
				return "TRUE";
			} else if (value == "false" || value == "0") {
				return "FALSE";
			}
			return "NULL";
		} else if (columnType == SQLDataType.DATE) {
			if (!DATE_PATTERN.matcher(value).matches()) {
				return "NULL";
			}
			if (dbTypeIs(MSSQL) || dbTypeIs(MSAccess)) {
				// TODO: Reportedly, MS Access requires "#2006-09-15#" (?)
				return singleQuote(value);
			}
			return "DATE " + singleQuote(value);
		} else if (columnType == SQLDataType.TIMESTAMP) {
			value = value.replace('T', ' ').replace("Z", "+00:00");
			if (!TIMESTAMP_PATTERN.matcher(value).matches()) {
				return "NULL";
			}
			if (dbTypeIs(MSSQL) || dbTypeIs(MSAccess)) {
				// TODO: Reportedly, MS Access requires "#2006-09-15 23:59:00#" (?)
				return singleQuote(value);
			}
			return "TIMESTAMP " + singleQuote(value);
		} else if (columnType == SQLDataType.TIME) {
			value = value.replace("Z", "+00:00");
			if (!TIME_PATTERN.matcher(value).matches()) {
				return "NULL";
			}
			if (dbTypeIs(MSSQL) || dbTypeIs(MSAccess)) {
				// TODO: Reportedly, MS Access requires "#23:59:00#" (?)
				return singleQuote(value);
			}
			return "TIME " + singleQuote(value);
		} else if (columnType == SQLDataType.BINARY) {
			if (!BINARY_PATTERN.matcher(value).matches()) {
				return "NULL";
			}
			// Value is assumed to be a hex string, as per xsd:hexBinary
			if (dbTypeIs(Oracle)) {
				return singleQuote(value);
			} else if (dbTypeIs(MSSQL)) {
				return "0x" + value;
			} else if (dbTypeIs(PostgreSQL)) {
				return "E'\\\\x" + value + "'";
			} else {
				return "X" + singleQuote(value);
			}
		} else if (columnType == SQLDataType.BIT) {
			if (dbTypeIs(MSSQL)) {
				// On SQL Server, BIT is a single-bit numeric type
				try {
					return Integer.parseInt(value) == 0 ? "0" : "1";
				} catch (NumberFormatException nfex) {
					// Not 0 or 1
					return "NULL";
				}
			} else {
				// In SQL-92, BIT is a bit string with a special literal form
				if (!value.matches("^[01]*$")) return "NULL";
				return "B" + singleQuote(value);
			}
		}
		// Default
		return singleQuote(value);
	}
	
	public String quoteValue(String value, Attribute column) {
	    return quoteValue(value, columnType(column));
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
	
	public boolean isIgnoredTable(String schema, String table) {
		// PostgreSQL has schemas "information_schema" and "pg_catalog" in every DB
		if (this.dbTypeIs(ConnectedDB.PostgreSQL))
			return Arrays.binarySearch(POSTGRESQL_IGNORED_SCHEMAS, schema) >= 0;

		// Skip Oracle system schemas as well as deleted tables in Oracle's Recycling Bin.
		// The latter have names like MYSCHEMA.BIN$FoHqtx6aQ4mBaMQmlTCPTQ==$0
		if (this.dbTypeIs(ConnectedDB.Oracle))
			return Arrays.binarySearch(ORACLE_IGNORED_SCHEMAS, schema) >= 0 || table.startsWith("BIN$");
			
		// MS SQL Server has schemas "sys" and "information_schema" in every DB
        // along with tables which need to be ignored
		if (this.dbTypeIs(ConnectedDB.MSSQL))
			return MSSQL_IGNORED_SCHEMAS.contains(schema) || "sysdiagrams".equals(table);

		return false;
	}

	/**
	 * Closes the database connection and shuts down the keep alive agent.
	 */
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

	/**
	 * Pre-registers a JDBC driver if its class can be found on the
	 * classpath. If the class is not found, nothing will happen.
	 * @param driverClassName Fully qualified class name of a JDBC driver
	 */
	public static void registerJDBCDriverIfPresent(String driverClassName) {
		if (driverClassName == null) return;
		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException ex) {
			// not present, just ignore this driver
		}
	}

	/**
	 * Tries to guess the class name of a suitable JDBC driver from a JDBC URL.
	 * This only works in the unlikely case that the driver has been registered
	 * earlier using Class.forName(classname).
	 * @param jdbcURL A JDBC URL
	 * @return The corresponding JDBC driver class name, or <tt>null</tt> if not known
	 */
	public static String guessJDBCDriverClass(String jdbcURL) {
		try {
			return DriverManager.getDriver(jdbcURL).getClass().getName();
		} catch (SQLException ex) {
			return null;
		}
	}

	/**
	 * Registers a JDBC driver class.
	 * @param driverClassName Fully qualified class name of a JDBC driver
	 * @throws D2RQException If the class could not be found
	 */
	public static void registerJDBCDriver(String driverClassName) {
		if (driverClassName == null) return;
		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException ex) {
			throw new D2RQException("Database driver class not found: " + driverClassName,
					D2RQException.DATABASE_JDBCDRIVER_CLASS_NOT_FOUND);
		}
	}
}
