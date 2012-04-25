package de.fuberlin.wiwiss.d2rq.map;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType.GenericType;
import de.fuberlin.wiwiss.d2rq.sql.SQLScriptLoader;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;


/**
 * Representation of a d2rq:Database from the mapping file.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Database extends MapObject {
	public static final int NO_LIMIT = -1;
	public static final int NO_FETCH_SIZE = -1;	
	
	private String jdbcDSN;
	private String jdbcDriver;
	private String username;
	private String password;
	private final Map<String,GenericType> columnTypes = new HashMap<String,GenericType>();
    private int limit = NO_LIMIT;
    private int fetchSize = NO_FETCH_SIZE;
	private String startupSQLScript = null;
	private ConnectedDB connection = null;
	private Properties connectionProperties = new Properties();
	
	public Database(Resource resource) {
		super(resource);
	}

	public void setJDBCDSN(String jdbcDSN) {
		assertNotYetDefined(this.jdbcDSN, D2RQ.jdbcDSN,
				D2RQException.DATABASE_DUPLICATE_JDBCDSN);
		checkNotConnected();		
		this.jdbcDSN = jdbcDSN;
	}
	
	public String getJDBCDSN() {
		return this.jdbcDSN;
	}

	public void setJDBCDriver(String jdbcDriver) {
		assertNotYetDefined(this.jdbcDriver, D2RQ.jdbcDriver,
				D2RQException.DATABASE_DUPLICATE_JDBCDRIVER);
		checkNotConnected();		
		this.jdbcDriver = jdbcDriver;
	}

	public String getJDBCDriver() {
		return jdbcDriver;
	}
	
	public void setUsername(String username) {
		assertNotYetDefined(this.username, D2RQ.username,
				D2RQException.DATABASE_DUPLICATE_USERNAME);
		checkNotConnected();		
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
	
	public void setPassword(String password) {
		assertNotYetDefined(this.password, D2RQ.password,
				D2RQException.DATABASE_DUPLICATE_PASSWORD);
		checkNotConnected();		
		this.password = password;
	}

	public String getPassword() {
		return password;
	}
	
	public void addTextColumn(String column) {
		checkNotConnected();		
		columnTypes.put(column, GenericType.CHARACTER);
	}
	
	public void addNumericColumn(String column) {
		checkNotConnected();		
		columnTypes.put(column, GenericType.NUMERIC);
	}
	
	public void addBooleanColumn(String column) {
		checkNotConnected();		
		columnTypes.put(column, GenericType.BOOLEAN);
	}
	
	public void addDateColumn(String column) {
		checkNotConnected();		
		columnTypes.put(column, GenericType.DATE);
	}
	
	public void addTimestampColumn(String column) {
		checkNotConnected();		
		columnTypes.put(column, GenericType.TIMESTAMP);
	}
	
	public void addTimeColumn(String column) {
		checkNotConnected();		
		columnTypes.put(column, GenericType.TIME);
	}
	
	public void addBinaryColumn(String column) {
		checkNotConnected();
		columnTypes.put(column, GenericType.BINARY);
	}
	
	public void addBitColumn(String column) {
		checkNotConnected();
		columnTypes.put(column, GenericType.BIT);
	}
	
	public void addIntervalColumn(String column) {
		checkNotConnected();
		columnTypes.put(column, GenericType.INTERVAL);
	}
	
	public void setResultSizeLimit(int limit) {
		checkNotConnected();		
		this.limit = limit;
	}
	
	public int getResultSizeLimit() {
		return limit;
	}
	
	public int getFetchSize() {
		return this.fetchSize;
	}
	
	public void setFetchSize(int fetchSize) {
		checkNotConnected();		
		this.fetchSize = fetchSize;
	}
	
	public void setStartupSQLScript(Resource script) {
		checkNotConnected();
		assertNotYetDefined(startupSQLScript, D2RQ.startupSQLScript, 
				D2RQException.DATABASE_DUPLICATE_STARTUPSCRIPT);
		startupSQLScript = script.getURI();
	}
	
	public void setConnectionProperty(String key, String value) {
		checkNotConnected();		
		this.connectionProperties.setProperty(key, value);
	}
	
	/**
	 * This is a hack where we can pass a pre-existing ConnectedDB that
	 * will be used by this Database, so we avoid that the Database
	 * opens another connection to the same DB.
	 */
	public void useConnectedDB(ConnectedDB db) {
		this.connection = db;
	}
	
	public ConnectedDB connectedDB() {
		if (this.connection == null) {
			if (jdbcDriver != null) {
				ConnectedDB.registerJDBCDriver(jdbcDriver);
			}
			connection = new ConnectedDB(jdbcDSN, username, password,
					columnTypes, limit, fetchSize, connectionProperties);
			if (startupSQLScript != null) {
				try {
					URI url = URI.create(startupSQLScript);
					SQLScriptLoader.loadURI(url, connection.connection());
				} catch (IOException ex) {
					connection.close();
					throw new D2RQException(ex);
				} catch (SQLException ex) {
					connection.close();
					throw new D2RQException(ex);
				}
			}
		}
		return connection;
	}

	public String toString() {
		return "d2rq:Database " + super.toString();
	}

	public void validate() throws D2RQException {
		if (this.jdbcDSN == null) {
			throw new D2RQException("d2rq:Database must have d2rq:jdbcDSN",
					D2RQException.DATABASE_MISSING_DSN);
		}
		if (this.jdbcDSN != null && this.jdbcDriver == null) {
			throw new D2RQException("Missing d2rq:jdbcDriver",
					D2RQException.DATABASE_MISSING_JDBCDRIVER);
		}
		// TODO
	}
	
	private void checkNotConnected() {
		if (this.connection != null) {
			throw new D2RQException("Cannot modify Database as it is already connected",
					D2RQException.DATABASE_ALREADY_CONNECTED);
		}
	}
}