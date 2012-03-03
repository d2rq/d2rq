package de.fuberlin.wiwiss.d2rq.helpers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A simple wrapper around a HSQL in-memory database for testing purposes
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class HSQLDatabase {
	public final static String HSQL_DRIVER_CLASS = org.hsqldb.jdbcDriver.class.getName();

	private final static String HSQL_USER = "d2rq";
	private final static String HSQL_PASS = "";
	
	private final String jdbcURL;
	private final Connection conn;
	
	public HSQLDatabase(String databaseName) {
		jdbcURL = "jdbc:hsqldb:mem:" + databaseName;
		try {
			Class.forName(HSQL_DRIVER_CLASS);
			conn = DriverManager.getConnection(jdbcURL + ";create=true", 
					HSQL_USER, HSQL_PASS);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public String getJdbcURL() {
		return jdbcURL;
	}
	
	public String getUser() {
		return HSQL_USER;
	}
	
	public String getPassword() {
		return HSQL_PASS;
	}
	
	public void executeSQL(String sql) {
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public PreparedStatement prepareSQL(String sql) throws SQLException {
		return conn.prepareStatement(sql);
	}
	
	public void clear() {
		executeSQL("DROP SCHEMA PUBLIC CASCADE");
	}
	
	public void close() {
		try {
			conn.close();
		} catch (SQLException ex) {
			// do nothing
		}
	}
	
	public void close(boolean dropAll) {
		if (dropAll) {
			clear();
		}
		close();
	}
}
