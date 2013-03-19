package org.d2rq.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.D2RQException;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.OpVisitor;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Inspector;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.lang.Database;

 
/**
 * TODO: Move all engine-specific code from here to {@link Vendor} and its implementations
 * TODO: Move keepalive agent into a separate class
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author kurtjx (http://github.com/kurtjx)
 */
public class SQLConnection {
	private static final Log log = LogFactory.getLog(SQLConnection.class);

	{
		SQLConnection.registerJDBCDriverIfPresent("com.mysql.jdbc.Driver");
		SQLConnection.registerJDBCDriverIfPresent("org.postgresql.Driver");
		SQLConnection.registerJDBCDriverIfPresent("org.hsqldb.jdbcDriver");
	}
	
	public static final String KEEP_ALIVE_PROPERTY = "keepAlive"; // interval property, value in seconds
	public static final int DEFAULT_KEEP_ALIVE_INTERVAL = 60*60; // hourly
	public static final String KEEP_ALIVE_QUERY_PROPERTY = "keepAliveQuery"; // override default keep alive query
	public static final String DEFAULT_KEEP_ALIVE_QUERY = "SELECT 1"; // may not work for some DBMS
	
	private final String jdbcURL;
	private final String jdbcDriverClass;
	private final String username;
	private final String password;
	private final Properties connectionProperties;
	private int limit;
	private int fetchSize = Database.NO_FETCH_SIZE;
	private int defaultFetchSize = Database.NO_FETCH_SIZE;

	private Connection connection = null;
	private Inspector schemaInspector = null;
	// Lazy initialization -- use vendor() for access!
	private Vendor vendor = null;
	private Map<ColumnName,Boolean> zerofillCache = 
		new HashMap<ColumnName,Boolean>();

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
					log.info("Connection will be reset since a failure is detected by keep alive agent.");
					if (s != null) { try { s.close(); } catch (Exception ignore) {} } 
					resetConnection();
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

	private void resetConnection() {
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (SQLException sqlExc) {
				// ignore...
				log.error("Error while closing current connection: "
						+ sqlExc.getMessage(), sqlExc);
			} finally {
				this.connection = null;
			}
		}
	}

	private final KeepAliveAgent keepAliveAgent;

	public SQLConnection(String jdbcURL, String jdbcDriver, String username, String password) {
		this(jdbcURL, jdbcDriver, username, password, null);
	}

	public SQLConnection(String jdbcURL, String jdbcDriver, String username, String password, Properties connectionProperties) {
		this.jdbcURL = jdbcURL;
		this.jdbcDriverClass = jdbcDriver;
		this.username = username;
		this.password = password;
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
	
	public String getJdbcDriverClass() {
		return jdbcDriverClass;
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
	
	private final Map<String,SQLOp> selectStatementCache =
		new HashMap<String,SQLOp>();
	private final Map<String,String> errorCache =
		new HashMap<String,String>();
	private final Map<TableName,TableOp> tableCache =
		new HashMap<TableName,TableOp>();
	private final Map<String,Collection<TableName>> tableNames = 
		new HashMap<String,Collection<TableName>>();
	private final Map<TableName,Boolean> isReferencedCache =
		new HashMap<TableName,Boolean>();
	
	private void cacheSelectStatement(String sql) {
		if (selectStatementCache.containsKey(sql) || errorCache.containsKey(sql)) return;
		try {
			selectStatementCache.put(sql, 
					new SQLOp(this, sql, metadata().describeSelectStatement(sql)));
		} catch (D2RQException ex) {
			if (ex.errorCode() == D2RQException.D2RQ_SQLEXCEPTION) {
				errorCache.put(sql, ex.getCause().getMessage());
			} else {
				throw ex;
			}
		}
	}
	
	public String getParseError(String sql) {
		cacheSelectStatement(sql);
		return errorCache.get(sql);
	}

	public SQLOp getSelectStatement(String sql) {
		cacheSelectStatement(sql);
		return selectStatementCache.get(sql);
	}

	/**
	 * Lists available table names. Caches results.
	 * 
	 * @param searchInSchema Schema to list tables from; <tt>null</tt> to list tables from all schemas
	 * @return A list of table names
	 */
	public Collection<TableName> getTableNames(String searchInSchema) {
		String key = searchInSchema == null ? "" : searchInSchema;
		if (!tableNames.containsKey(key)) {
			tableNames.put(key, metadata().getTableNames(searchInSchema));
		}
		return tableNames.get(key);
	}

	private void cacheTable(TableName table) {
		if (tableCache.containsKey(table)) return;
		TableDef tableDef = metadata().describeTableOrView(table);
		if (tableDef != null) {
			tableCache.put(table, new TableOp(tableDef));
		}
	}
	
	public boolean isTable(TableName table) {
		cacheTable(table);
		return tableCache.get(table) != null;
	}
	
	/**
	 * @param table A table name
	 * @return Metadata about the table, or <code>null</code> if it doesn't exist
	 */
	public TableOp getTable(TableName table) {
		cacheTable(table);
		return tableCache.get(table);
	}
	
	/**
	 * @return <code>true</code> if another table has a foreign key referencing
	 * 		this table's primary key
	 */
	public boolean isReferencedByForeignKey(TableName table) {
		if (!isReferencedCache.containsKey(table)) {
			isReferencedCache.put(table, metadata().isReferencedByForeignKey(table));
		}
		return isReferencedCache.get(table);
	}
	
	public int limit() {
		return this.limit;
	}
	
	public void setLimit(int resultSizeLimit) {
		this.limit = resultSizeLimit;
	}

	/**
	 * @param fetchSize Value specified in user config or mapping file
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * @param value Default value for the current operation mode (e.g., increase for dumps)
	 */
	public void setDefaultFetchSize(int value) {
		defaultFetchSize = value;
	}
	
	public int fetchSize() {
		if (fetchSize == Database.NO_FETCH_SIZE) {
			if (vendor() == Vendor.MySQL) {
				return Integer.MIN_VALUE;
			}
			return defaultFetchSize;
		}
		return fetchSize;
	}

	private void connect() {
		if (jdbcURL != null && !jdbcURL.toLowerCase().startsWith("jdbc:")) {
			throw new D2RQException("Not a JDBC URL: " + jdbcURL, D2RQException.D2RQ_DB_CONNECTION_FAILED);
		}
		try {
			log.info("Establishing JDBC connection to " + jdbcURL);
			if (jdbcDriverClass != null) {
				try {
					Class.forName(jdbcDriverClass);
				} catch (ClassNotFoundException ex) {
					throw new D2RQException("Database driver class not found: " + jdbcDriverClass,
							D2RQException.DATABASE_JDBCDRIVER_CLASS_NOT_FOUND);
				}
			}
			this.connection = DriverManager.getConnection(this.jdbcURL, getConnectionProperties());
		} catch (SQLException ex) {
			close();
			throw new D2RQException(
					"Database connection to " + jdbcURL + " failed " +
					"(user: " + username + "): " + ex.getMessage(), 
					D2RQException.D2RQ_DB_CONNECTION_FAILED);
		}
		// Database-dependent initialization
		try {
			vendor().initializeConnection(connection);
		} catch (SQLException ex) {
			close();
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
	
	private Inspector metadata() {
		if (schemaInspector == null && jdbcURL != null) {
			schemaInspector = new Inspector(connection(), vendor());
		}
		return this.schemaInspector;
	}

	/**
	 * @return A helper for generating SQL statements conforming to the syntax
	 * of the database engine used in this connection
	 */
	public Vendor vendor() {
		ensureVendorInitialized();
		return vendor;
	}

	protected String getDatabaseProductType() throws SQLException {
		return connection().getMetaData().getDatabaseProductName();
	}
	
	private void ensureVendorInitialized() {
		if (vendor != null) return;
		try {
			String productName = getDatabaseProductType();
			log.info("JDBC database product type: " + productName);
			productName = productName.toLowerCase();
			if (productName.indexOf("mysql") >= 0) {
				vendor = Vendor.MySQL;
			} else if (productName.indexOf("postgresql") >= 0) {
				vendor = Vendor.PostgreSQL;
			} else if (productName.indexOf("interbase") >= 0) {
				vendor = Vendor.InterbaseOrFirebird;
			} else if (productName.indexOf("oracle") >= 0) {
				this.vendor = Vendor.Oracle; 
			} else if (productName.indexOf("microsoft sql server") >= 0) {
				this.vendor = Vendor.SQLServer;
			} else if (productName.indexOf("access") >= 0) {
				this.vendor = Vendor.MSAccess;
			} else if (productName.contains("sybase") || 
					productName.contains("adaptive server enterprise") ||
					"ase".equals(productName)) {
				this.vendor = Vendor.Sybase;
			} else if (productName.indexOf("hsql") >= 0) {
				this.vendor = Vendor.HSQLDB;
			} else {
				this.vendor = Vendor.SQL92;
			}
			log.info("Using vendor class: " + vendor.getClass().getName());
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
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
	public boolean areCompatibleFormats(DatabaseOp table1, ColumnName column1, 
			DatabaseOp table2, ColumnName column2) {
		// TODO Right now we only catch the ZEROFILL case. There are many more!
		return !isZerofillColumn(table1, column1) && 
				!isZerofillColumn(table2, column2);
	}
	
	private boolean isZerofillColumn(final DatabaseOp table, final ColumnName column) {
		if (vendor() != Vendor.MySQL || !table.hasColumn(column)) return false;
		return new OpVisitor.Default(true) {
			{ table.accept(this); }
			ColumnName realColumn = column;
			boolean result = false;
			@Override
			public boolean visitEnter(AliasOp table) {
				realColumn = table.getOriginalColumnName(realColumn);
				return true;
			}
			@Override
			public void visit(TableOp table) {
				if (!zerofillCache.containsKey(realColumn)) {
					zerofillCache.put(realColumn, metadata().isZerofillColumn(realColumn));
				}
				result = zerofillCache.get(column);
			}
			@Override
			public void visit(SQLOp table) {
				result = false;
			}
		}.result;
	}
	
	/**
	 * Closes the database connection and shuts down the keep alive agent.
	 */
	public void close() {
		if (keepAliveAgent != null)
			keepAliveAgent.shutdown();
		
		if (connection != null) try {
			log.info("Closing connection to " + jdbcURL);
			this.connection.close();
		} catch (SQLException sqlExc) {
			// ignore...
			log.error("Error while closing current connection: "
					+ sqlExc.getMessage(), sqlExc);
		} finally {
			connection = null;
		}
	}
	
    public boolean equals(Object other) {
    	if (!(other instanceof SQLConnection)) {
    		return false;
    	}
    	return jdbcURL.equals(((SQLConnection) other).jdbcURL);
    }
    
    public int hashCode() {
    	return jdbcURL.hashCode();
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
}
