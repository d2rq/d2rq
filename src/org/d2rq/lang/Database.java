package org.d2rq.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.d2rq.D2RQException;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.vocab.D2RQ;

import com.hp.hpl.jena.rdf.model.Resource;



/**
 * Representation of a d2rq:Database from the mapping file.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Database extends MapObject {
	public static final int NO_LIMIT = -1;
	public static final int NO_FETCH_SIZE = -1;	
	
	private String jdbcURL;
	private String jdbcDriver;
	private String username;
	private String password;
	private final Map<ColumnName,GenericType> columnTypes = 
		new HashMap<ColumnName,GenericType>();
    private int limit = NO_LIMIT;
    private int fetchSize = NO_FETCH_SIZE;
	private String startupSQLScript = null;
	private Properties connectionProperties = new Properties();
	
	public Database(Resource resource) {
		super(resource);
	}

	public void setJdbcURL(String jdbcURL) {
		assertNotYetDefined(this.jdbcURL, D2RQ.jdbcURL,
				D2RQException.DATABASE_DUPLICATE_JDBC_URL);
		this.jdbcURL = jdbcURL;
	}
	
	public String getJdbcURL() {
		return this.jdbcURL;
	}

	public void setJDBCDriver(String jdbcDriver) {
		assertNotYetDefined(this.jdbcDriver, D2RQ.jdbcDriver,
				D2RQException.DATABASE_DUPLICATE_JDBCDRIVER);
		this.jdbcDriver = jdbcDriver;
	}

	public String getJDBCDriver() {
		return jdbcDriver;
	}
	
	public void setUsername(String username) {
		assertNotYetDefined(this.username, D2RQ.username,
				D2RQException.DATABASE_DUPLICATE_USERNAME);
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
	
	public void setPassword(String password) {
		assertNotYetDefined(this.password, D2RQ.password,
				D2RQException.DATABASE_DUPLICATE_PASSWORD);
		this.password = password;
	}

	public String getPassword() {
		return password;
	}
	
	public void addTextColumn(String column) {
		addColumn(column, GenericType.CHARACTER);
	}
	
	public void addNumericColumn(String column) {
		addColumn(column, GenericType.NUMERIC);
	}
	
	public void addBooleanColumn(String column) {
		addColumn(column, GenericType.BOOLEAN);
	}
	
	public void addDateColumn(String column) {
		addColumn(column, GenericType.DATE);
	}
	
	public void addTimestampColumn(String column) {
		addColumn(column, GenericType.TIMESTAMP);
	}
	
	public void addTimeColumn(String column) {
		addColumn(column, GenericType.TIME);
	}
	
	public void addBinaryColumn(String column) {
		addColumn(column, GenericType.BINARY);
	}
	
	public void addBitColumn(String column) {
		addColumn(column, GenericType.BIT);
	}
	
	public void addIntervalColumn(String column) {
		addColumn(column, GenericType.INTERVAL);
	}
	
	private void addColumn(String column, GenericType type) {
		columnTypes.put(Microsyntax.parseColumn(column), type);
	}

	public Map<ColumnName,GenericType> getColumnTypes() {
		return columnTypes;
	}
	
	public void setResultSizeLimit(int limit) {
		this.limit = limit;
	}
	
	public int getResultSizeLimit() {
		return limit;
	}
	
	public int getFetchSize() {
		return this.fetchSize;
	}
	
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}
	
	public String getStartupSQLScript() {
		return startupSQLScript;
	}
	
	public void setStartupSQLScript(Resource script) {
		assertNotYetDefined(startupSQLScript, D2RQ.startupSQLScript, 
				D2RQException.DATABASE_DUPLICATE_STARTUPSCRIPT);
		startupSQLScript = script.getURI();
	}
	
	public Properties getConnectionProperties() {
		return connectionProperties;
	}
	
	public void setConnectionProperty(String key, String value) {
		this.connectionProperties.setProperty(key, value);
	}
	
	public String toString() {
		return "d2rq:Database " + super.toString();
	}

	public void accept(D2RQMappingVisitor visitor) {
		visitor.visit(this);
	}
}