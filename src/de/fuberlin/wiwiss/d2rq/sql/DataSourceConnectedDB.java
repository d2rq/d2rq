package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;


/**
 * A DataSource based database
 * 
 * This implementation of {@link ConnectedDB} uses a JDBC <code>DataSource</code> object to create database connections.
 * 
 * This allows D2R to use container managed connection pools. It therefore does not use or needs a keep alive thread. 
 * 
 * @author G. Mels
 */
public class DataSourceConnectedDB extends ConnectedDB {
	
	private final DataSource dataSource;
	
	private static final Log logger = LogFactory.getLog(DataSourceConnectedDB.class);
	
	public DataSourceConnectedDB(DataSource dataSource, boolean allowDistinct,
	                             Map<String,SQLDataType> columnTypes,
	                             int limit, int fetchSize) {
		super(allowDistinct, columnTypes, limit, fetchSize);
		
		this.dataSource = dataSource;
	}
	
	/**
	 * Does nothing in this implementation.
	 */
	@Override
	public void init() {
		// nop
	}
	
	/**
	 * Obtains a new <code>Connection</code> to the database.
	 * 
	 * @return a new <code>Connection</code> to the database.
	 */
	@Override
	public Connection connection() {
		try {
			long now = 0L;
			
			if (logger.isDebugEnabled()) {
				logger.debug("opening connection to " + dataSource);
				now = System.currentTimeMillis();
			}
			
			Connection connection = dataSource.getConnection();
			
			if (logger.isDebugEnabled()) {
				logger.debug("connection opened in " + new Long((System.currentTimeMillis() - now)) + " ms");
			}
			
			return connection;
		} catch (SQLException e) {
			logger.error("opening connection failed: code " +  String.valueOf(e.getErrorCode()) + " state " + e.getSQLState());
			throw new D2RQException("Opening database connection failed: " + String.valueOf(e.getErrorCode()), e, D2RQException.D2RQ_DB_CONNECTION_FAILED);
		}
	}
	
	/**
	 * Closes the given connection.
	 * 
	 * When the connection is part of a connection pool, this will return the connection to the pool.
	 * 
	 * @param c The connection to close.
	 */
	@Override
	public void close(Connection c) {
		if (c != null) {
			logger.debug("closing connection");
			try { c.close(); } catch (SQLException e) { /* ignore */ }
		}
	}
	
	/**
	 * Does nothing in this implementation.
	 */
	@Override
	public void close() {
		// nop
	}
	
	public boolean equals(Object otherObject) {
		if (otherObject == null)
			return false;
		
		if (this == otherObject)
			return true;
		
		if (!(otherObject instanceof DataSourceConnectedDB)) {
			return false;
		}
		DataSourceConnectedDB other = (DataSourceConnectedDB) otherObject;
		return this.dataSource.equals(other.dataSource);
	}
	
	public int hashCode() {
		return this.dataSource.hashCode();
	}
	
	public String toString()
	{
		return this.dataSource.toString();
	}

}
