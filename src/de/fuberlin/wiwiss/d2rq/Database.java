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
public class Database {

    /** URI or bNodeId of the classMap from the mapping file. */
    private Node id;

    /** Flag that the database connection has been established */
    private boolean connectedToDatabase = false;

    /** Types of all columns used from this database.
     * Possible values are d2rq:numericColumn, d2rq:textColumn and d2rq:dateColumn.
    */
    private HashMap columnTypes;

    private String odbc;
    private String jdbc;
    private String jdbcDriver;
    private String databaseUsername;
    private String databasePassword;
    private Connection con;

     public Database(Node id, String odbc, String jdbc, String jdbcDriver, String databaseUsername, String databasePassword, HashMap  columnTypes) {
        this.id = id;
        this.odbc = odbc;
        this.jdbc =  jdbc;
        this.jdbcDriver = jdbcDriver;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.columnTypes = columnTypes;
    }

    /**
     * Returns a connection to this database.
     * @return connection
     */
    protected Connection getConnnection() {
        if(!connectedToDatabase) {
            this.connectToDatabase();
        }
        return this.con;
    }


    /**
     * Returns the ODBC data source name.
     * @return odbcDSN
     */
    protected String getOdbc() {
        return this.odbc;
    }

    /**
     * Returns the JDBC data source name.
     * @return jdbcDSN
     */
    protected String getJdbc() {
        return this.jdbc;
    }

    /**
     * Returns the JDBC driver.
     * @return jdbcDriver
     */
    protected String getJdbcDriver() {
        return this.jdbcDriver;
    }

    /**
     * Returns the database username.
     * @return username
     */
    protected String getDatabaseUsername() {
        return this.databaseUsername;
    }

    /**
     * Returns the database password.
     * @return password
     */
    protected String getDatabasePassword() {
        return this.databasePassword;
    }

    /**
     * Returns the columnType for a given database column.
     * @return Node columnType D2RQ.textColumn or D2RQ.numericColumn or D2RQ.dateColumn
     */
    protected Node getColumnType(String column) {
        Node type = (Node) this.columnTypes.get(column);
        if (type != null) {
            return type ;
        } else {
            System.err.println("D2RQ Error: The column " + column + " doesn't have a corresponding d2rq:numericColumn or d2rq:textColumn statement");
            return type;
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
                    throw new D2RQException("Could not connect to database because of missing JDBC driver.");
                }
            }
            if (url != "") {
                if (this.getDatabaseUsername() != null && this.getDatabasePassword() != null) {
                    con = DriverManager.getConnection(url, this.getDatabaseUsername(), this.getDatabasePassword());
                } else {
                    con = DriverManager.getConnection(url);
                }
            } else {
                throw new D2RQException("Could not connect to database because of missing URL.");
            }
            connectedToDatabase = true;
       }  catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
       }
    }
}
