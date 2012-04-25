package de.fuberlin.wiwiss.d2rq.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reads SQL statements from a file or other source.
 * 
 * Statements must end with semicolon and must end
 * at the end of a line. Lines starting
 * with -- are considered comments and are ignored.
 */
public class SQLScriptLoader {
	private final static Log log = LogFactory.getLog(SQLScriptLoader.class);
	
	/**
	 * Loads a SQL script from a file and executes it.
	 */
	public static void loadFile(File file, Connection conn) 
	throws FileNotFoundException, SQLException {
		try {
			log.info("Reading SQL script from " + file);
			new SQLScriptLoader(new InputStreamReader(
					new FileInputStream(file), "utf-8"), conn).execute();
		} catch (UnsupportedEncodingException ex) {
			// UTF-8 is always supported
		}
	}
	
	/**
	 * Loads a SQL script from a URL and executes it.
	 */
	public static void loadURI(URI url, Connection conn) 
	throws IOException, SQLException {
		try {
			log.info("Reading SQL script from <" + url + ">");
			new SQLScriptLoader(new InputStreamReader(
					url.toURL().openStream(), "utf-8"), conn).execute();
		} catch (UnsupportedEncodingException ex) {
			// UTF-8 is always supported
		}
	}
	private final BufferedReader in;
	private final Connection conn;
	
	public SQLScriptLoader(Reader in, Connection conn) {
		this.in = new BufferedReader(in);
		this.conn = conn;
	}
	
	public void execute() throws SQLException {
		int lineNumber = 1;
		int statements = 0;
		Statement stmt = conn.createStatement();
		try {
			String line;
			StringBuilder sql = new StringBuilder();
			while ((line = in.readLine()) != null) {
				if (line.trim().startsWith("--")) {
					// comment, ignore this line
				} else {
					if (line.trim().endsWith(";")) {
						sql.append(line.substring(0, line.length() -1));
						String s = sql.toString().trim();
						if (!"".equals(s)) {
							stmt.execute(s);
							statements++;
						}
						sql = new StringBuilder();
					} else {
						sql.append(line);
						sql.append('\n');
					}
				}
				lineNumber++;
			}
			String s = sql.toString().trim();
			if (!"".equals(s)) {
				stmt.execute(s);
			}
			log.info("Done, " + (lineNumber - 1) + " lines, " + statements + " statements");
		} catch (SQLException ex) {
			throw new SQLException(
					"in line " + lineNumber + ": " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			stmt.close();
		}
	}
}
