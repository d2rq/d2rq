package de.fuberlin.wiwiss.d2rq.sql;

import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.map.Database;



public class DriverConnectedDB extends ConnectedDB {

	private static final Log log = LogFactory.getLog(DriverConnectedDB.class);
	
	
	public static final String KEEP_ALIVE_PROPERTY = "keepAlive"; // interval property, value in seconds
	public static final int DEFAULT_KEEP_ALIVE_INTERVAL = 60*60; // hourly
	public static final String KEEP_ALIVE_QUERY_PROPERTY = "keepAliveQuery"; // override default keep alive query
	public static final String DEFAULT_KEEP_ALIVE_QUERY = "SELECT 1"; // may not work for some DBMS
	
	private static final String ORACLE_SET_DATE_FORMAT = "ALTER SESSION SET NLS_DATE_FORMAT = 'SYYYY-MM-DD'";
	private static final String ORACLE_SET_TIMESTAMP_FORMAT = "ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'SYYYY-MM-DD HH24:MI:SS'";
	
	private String jdbcURL;
	private String username;
	private String password;
	
	private final Properties connectionProperties;
	
	private String startupSQLScript;
	
	private Connection connection = null;
	
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
	
	public DriverConnectedDB(String jdbcURL, String username, String password) {
		this(jdbcURL, username, password, true,
	         Collections.<String,SQLDataType>emptyMap(),
	         Database.NO_LIMIT, Database.NO_FETCH_SIZE, null, null);
	}
	
	public DriverConnectedDB(String jdbcURL, String username, String password, String startupSQLScript) {
		this(jdbcURL, username, password, true,
	         Collections.<String,SQLDataType>emptyMap(),
	         Database.NO_LIMIT, Database.NO_FETCH_SIZE, null, startupSQLScript);
	}
	
	public DriverConnectedDB(String jdbcURL, String username, String password,
	                         boolean allowDistinct, Map<String,SQLDataType> columnTypes, int limit, int fetchSize,
	                         Properties connectionProperties, String startupSQLScript) {
		super(allowDistinct, columnTypes, limit, fetchSize);
		
		this.jdbcURL  = jdbcURL;
		this.username = username;
		this.password = password;
		this.connectionProperties = connectionProperties;
		this.startupSQLScript     = startupSQLScript;
		
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
	
	/**
	 * Opens a single persistent connection to the database.
	 */
	@Override
	public void init() {
		if (this.connection == null)
			connect();	
		
		if (startupSQLScript != null) {
			try {
				URI url = URI.create(startupSQLScript);
				SQLScriptLoader.loadURI(url, this.connection());
			} catch (IOException ex) {
				throw new D2RQException("Error accessing SQL startup script: " + startupSQLScript, ex);
			} catch (SQLException ex) {
				throw new D2RQException("Error importing " + startupSQLScript + " " + ex.getMessage(), ex, D2RQException.D2RQ_SQLEXCEPTION);
			}
		}
	}

	/**
	 * Returns the connection.
	 * 
	 * @return the database connection.
	 */
	@Override
	public Connection connection()
	{
		return this.connection;
	}
	
	/**
	 * This method does nothing in this implementation.
	 */
	@Override
	public void close(Connection connection)
	{
		// do nothing
	}
	
	/**
	 * Closes the database connection and shuts down the keep alive agent.
	 */
	@Override
	public void close()
	{
		if (keepAliveAgent != null)
			keepAliveAgent.shutdown();
		
		if (connection != null) try {
			log.info("closing connection");
			this.connection.close();
		} catch (SQLException ex) {
			throw new D2RQException(ex);
		}
	}
	
	private void connect() {
		try {
			long now = 0L;
		
			if (log.isDebugEnabled()) {
				now = System.currentTimeMillis();
				log.debug("opening connection to " + jdbcURL);
			}
			
			this.connection = DriverManager.getConnection(this.jdbcURL, getConnectionProperties());
			if (log.isDebugEnabled()) {
				log.debug("connection opened in " + new Long(System.currentTimeMillis() - now) + " ms");
			}
		} catch (SQLException ex) {
			throw new D2RQException(
					"Database connection to " + jdbcURL + " failed " +
					"(user: " + username + "): " + ex.getMessage(), 
					D2RQException.D2RQ_DB_CONNECTION_FAILED);
		}
		
		dbSpecificInit();
	}
	
	
	protected void dbSpecificInit()
	{
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
	
	
	
	public String getJdbcURL() {
		return jdbcURL;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	
    public boolean equals(Object otherObject) {
		if (otherObject == null)
			return false;
		
		if (this == otherObject)
			return true;
		
    	if (!(otherObject instanceof DriverConnectedDB)) {
    		return false;
    	}
    	DriverConnectedDB other = (DriverConnectedDB) otherObject;
    	return this.jdbcURL.equals(other.jdbcURL);
    }
    
    public int hashCode() {
    	return this.jdbcURL.hashCode();
    }
    
	public String toString()
	{
		return this.jdbcURL;
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
