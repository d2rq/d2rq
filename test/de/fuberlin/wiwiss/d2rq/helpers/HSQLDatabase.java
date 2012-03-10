package de.fuberlin.wiwiss.d2rq.helpers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
	
	public Connection getConnection() {
		return conn;
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
	
	/**
	 * Reads SQL statements from a file. Statements must end with
	 * semicolon and must end at the end of a line. Lines starting
	 * with # are considered comments and are ignored.
	 * 
	 * @param filename relative to current directory
	 */
	public void executeScript(String filename) {
		int lineNumber = 1;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf-8"));
			String line;
			StringBuilder sql = new StringBuilder();
			while ((line = in.readLine()) != null) {
				if (line.trim().startsWith("#")) {
					// comment, ignore this line
				} else {
					if (line.trim().endsWith(";")) {
						sql.append(line.substring(0, line.length() -1));
						executeSQL(sql.toString());
						sql = new StringBuilder();
					} else {
						sql.append(line);
						sql.append('\n');
					}
				}
				lineNumber++;
			}
		} catch (RuntimeException ex) {
			if (ex.getCause() instanceof SQLException) { 
				throw new RuntimeException(
						"in line " + lineNumber + ": " + ex.getMessage(), ex);
			}
			throw ex;
		} catch (UnsupportedEncodingException ex) {
			// UTF-8 is always supported
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public PreparedStatement prepareSQL(String sql) throws SQLException {
		return conn.prepareStatement(sql);
	}

	/**
	 * Returns the value of the first column of the first row of
	 * a <code>SELECT</code> SQL query result.
	 *  
	 * @param sql A <code>SELECT</code> query
	 * @return Value as a string, or null if there are zero 
	 * result rows or the value is <code>NULL</code>
	 */
	public String selectString(String sql) {
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (!rs.next()) return null;
			String result = rs.getString(1);
			rs.close();
			stmt.close();
			return result;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Returns the value of the first column of the first row of
	 * a <code>SELECT</code> SQL query result.
	 *  
	 * @param sql A <code>SELECT</code> query
	 * @return Value as an object, or null if there are zero 
	 * result rows or the value is <code>NULL</code>
	 */
	public Object selectObject(String sql) {
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (!rs.next()) return null;
			Object result = rs.getObject(1);
			rs.close();
			stmt.close();
			return result;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Returns the value of the first column of the first row of
	 * a <code>SELECT</code> SQL query result.
	 *  
	 * @param sql A <code>SELECT</code> query
	 * @return Value as a byte array, or null if there are zero 
	 * result rows or the value is <code>NULL</code>
	 */
	public byte[] selectBytes(String sql) {
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (!rs.next()) return null;
			byte[] result = rs.getBytes(1);
			rs.close();
			stmt.close();
			return result;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Returns the class name of the SQL value in the first column of the first row of
	 * a <code>SELECT</code> SQL query result.
	 *  
	 * @param sql A <code>SELECT</code> query
	 * @return The class name of the value, or null if there are zero 
	 * result rows or the value is <code>NULL</code>
	 */
	public String selectClassName(String sql) {
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (!rs.next()) return null;
			String result = rs.getMetaData().getColumnClassName(1);
			rs.close();
			stmt.close();
			return result;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Drops all tables.
	 */
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

	/**
	 * Closes the database.
	 * 
	 * @param dropAll Drops all tables if true.
	 */
	public void close(boolean dropAll) {
		if (dropAll) {
			clear();
		}
		close();
	}
}
