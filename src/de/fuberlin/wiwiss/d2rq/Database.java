/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import java.sql.*;
import java.util.*;

/** Representation of a d2rq:Database from the mapping file.
 * It contains the connection information for the database and the column types of all database columns used.
 *
 * <BR>History: 06-03-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
class Database {

    /** Flag that the database connection has been established */
    private boolean connectedToDatabase = false;

    /** Types of all columns used from this database.
     * Possible values are d2rq:numericColumn, d2rq:textColumn and d2rq:dateColumn.
    */
    private Map columnTypes;

    private String odbc;
    private String jdbc;
    private String jdbcDriver;
    private String databaseUsername;
    private String databasePassword;
    private Connection con;

    public Database(String odbc, String jdbc, String jdbcDriver, String databaseUsername, String databasePassword, Map columnTypes) {
        this.odbc = odbc;
        this.jdbc =  jdbc;
        this.jdbcDriver = jdbcDriver;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.columnTypes = columnTypes;
    }

    /**
     * Infers additional type information for columns used in a join
     * by assuming that two joined columns have the same type.
     * @param join try to infer column types based on this join
     */
	public void inferColumnTypes(Join join) {
		Iterator it = join.getFirstColumns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			inferColumnTypes(column.getQualifiedName(),
					join.getOtherSide(column).getQualifiedName());
		}
	}

	private void inferColumnTypes(String col1, String col2) {
		if (this.columnTypes.containsKey(col1)) {
			if (!this.columnTypes.containsKey(col2)) {
				this.columnTypes.put(col2, this.columnTypes.get(col1));
			}
		} else if (this.columnTypes.containsKey(col2)) {
			this.columnTypes.put(col1, this.columnTypes.get(col2));
		}
	}

    /**
     * Returns a connection to this database.
     * @return connection
     */
    public Connection getConnnection() {
    		if(!this.connectedToDatabase) {
    			connectToDatabase();
        }
        return this.con;
    }


    /**
     * Returns the ODBC data source name.
     * @return odbcDSN
     */
    public String getOdbc() {
        return this.odbc;
    }

    /**
     * Returns the JDBC data source name.
     * @return jdbcDSN
     */
    public String getJdbc() {
        return this.jdbc;
    }

    /**
     * Returns the JDBC driver.
     * @return jdbcDriver
     */
    public String getJdbcDriver() {
        return this.jdbcDriver;
    }

    /**
     * Returns the database username.
     * @return username
     */
    public String getDatabaseUsername() {
        return this.databaseUsername;
    }

    /**
     * Returns the database password.
     * @return password
     */
    public String getDatabasePassword() {
        return this.databasePassword;
    }

    /**
     * Returns the columnType for a given database column.
     * @return Node columnType D2RQ.textColumn or D2RQ.numericColumn or D2RQ.dateColumn
     */
    public Node getColumnType(Column column) {
		return (Node) this.columnTypes.get(column.getQualifiedName());
	}

    /**
     * Raises a D2RQ error if there's no type information for the column.
     * @param column a database column
     */
	public void assertHasType(Column column) {
		if (getColumnType(column) == null) {
			Logger.instance().error("The column " + column + " doesn't have a corresponding d2rq:numericColumn or d2rq:textColumn statement");
		}
    }

    private void connectToDatabase() {
       try {
            // Connect to database
            String url = "";
            if (this.odbc != null) {
                url = "jdbc:odbc:" + this.odbc;
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            } else if (this.jdbc != null) {
                url = this.jdbc;
                if (this.jdbcDriver != null) {
                    Class.forName(this.jdbcDriver);
                } else {
                    Logger.instance().error("Could not connect to database because of missing JDBC driver.");
                    return;
                }
            }
            if (url != "") {
                if (this.getDatabaseUsername() != null && this.getDatabasePassword() != null) {
                		this.con = DriverManager.getConnection(url, this.getDatabaseUsername(), this.getDatabasePassword());
                } else {
                		this.con = DriverManager.getConnection(url);
                }
            } else {
                Logger.instance().error("Could not connect to database because of missing URL.");
                return;
            }
            this.connectedToDatabase = true;
       }  catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
       }
    }
}
