package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType.GenericType;
import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;
 
/**
 * TODO Move all engine-specific code from here to {@link Vendor} and its implementations
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author kurtjx (http://github.com/kurtjx)
 */
public class ConnectedDB {
	private static final Log log = LogFactory.getLog(ConnectedDB.class);

	public static final String KEEP_ALIVE_PROPERTY = "keepAlive"; // interval property, value in seconds
	public static final int DEFAULT_KEEP_ALIVE_INTERVAL = 60*60; // hourly
	public static final String KEEP_ALIVE_QUERY_PROPERTY = "keepAliveQuery"; // override default keep alive query
	public static final String DEFAULT_KEEP_ALIVE_QUERY = "SELECT 1"; // may not work for some DBMS

	{
		ConnectedDB.registerJDBCDriverIfPresent("com.mysql.jdbc.Driver");
		ConnectedDB.registerJDBCDriverIfPresent("org.postgresql.Driver");
		ConnectedDB.registerJDBCDriverIfPresent("org.hsqldb.jdbcDriver");
	}

	private String jdbcURL;
	private String username;
	private String password;
	private final Map<Attribute,Boolean> cachedColumnNullability = 
		new HashMap<Attribute,Boolean>();
	private final Map<Attribute,DataType> cachedColumnTypes = 
		new HashMap<Attribute,DataType>();
	private final Map<Attribute,GenericType> overriddenColumnTypes =
		new HashMap<Attribute,GenericType>();
	private Connection connection = null;
	private DatabaseSchemaInspector schemaInspector = null;

	// Lazy initialization -- use vendor() for access!
	private Vendor vendor = null;

	private int limit;
	private int fetchSize;
	private int defaultFetchSize = Database.NO_FETCH_SIZE;
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
			Vendor v = null;
			while (!shutdown) {
				try { Thread.sleep(interval*1000); }
				catch (InterruptedException e) { if (shutdown) break; }

				try {
					if (log.isDebugEnabled())
						log.debug("Keep alive agent is executing noop query '" + query + "'...");
					c = connection();
					v = vendor();
					s = c.createStatement();

					v.beforeQuery(c);
					s.execute(query);
					v.afterQuery(c);
					
					v.beforeClose(c);
					s.close();
					v.afterClose(c);
					
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

	public ConnectedDB(String jdbcURL, String username, String password) {
		this(jdbcURL, username, password,
				Collections.<String,GenericType>emptyMap(),
				Database.NO_LIMIT, Database.NO_FETCH_SIZE, null);
	}

	public ConnectedDB(String jdbcURL, String username, String password, 
			Map<String,GenericType> columnTypes,
			int limit, int fetchSize, Properties connectionProperties) {
		// TODO replace column type arguments with a single column => type map
		this.jdbcURL = jdbcURL;
		this.username = username;
		this.password = password;
		this.limit = limit;
		this.fetchSize = fetchSize;
		this.connectionProperties = connectionProperties;

		for (String columnName: columnTypes.keySet()) {
			overriddenColumnTypes.put(SQL.parseAttribute(columnName), 
					columnTypes.get(columnName));
		}

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

	public void setDefaultFetchSize(int value) {
		defaultFetchSize = value;
	}

	public int fetchSize() {
		if (fetchSize == Database.NO_FETCH_SIZE) {
			if (vendorIs(Vendor.MySQL)) {
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

	public DatabaseSchemaInspector schemaInspector() {
		if (schemaInspector == null && jdbcURL != null) {
			schemaInspector = new DatabaseSchemaInspector(this);
		}
		return this.schemaInspector;
	}

	/**
	 * Returns a column's datatype. Caches the types for performance.
	 * @param column
	 * @return The column's datatype, or <code>null</code> if unknown
	 */
	public DataType columnType(Attribute column) {
		if (!cachedColumnTypes.containsKey(column)) {
			if (overriddenColumnTypes.containsKey(column)) {
				cachedColumnTypes.put(column, overriddenColumnTypes.get(column).dataTypeFor(vendor()));
			} else if (schemaInspector() == null) {
				cachedColumnTypes.put(column, GenericType.CHARACTER.dataTypeFor(vendor()));
			} else {
				cachedColumnTypes.put(column, schemaInspector().columnType(column));
			}
		}
		return cachedColumnTypes.get(column);
	}

	public boolean isNullable(Attribute column) {
		if (!cachedColumnNullability.containsKey(column)) {
			cachedColumnNullability.put(column, 
					schemaInspector() == null ? true : schemaInspector().isNullable(column));
		}
		return cachedColumnNullability.get(column);
	}

	/**
	 * Reports the brand of RDBMS.
	 * @return <tt>true</tt> if this database is of the given brand
	 * @see #vendor()
	 * 
	 * TODO make private, use {@link #vendor()} and its methods instead
	 */
	public boolean vendorIs(Vendor vendor) {
		return this.vendor.equals(vendor);
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
	public boolean areCompatibleFormats(Attribute column1, Attribute column2) {
		// TODO Right now we only catch the ZEROFILL case. There are many more!
		return !isZerofillColumn(column1) && !isZerofillColumn(column2);
	}

	private boolean isZerofillColumn(Attribute column) {
		if (!vendorIs(Vendor.MySQL)) return false;
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