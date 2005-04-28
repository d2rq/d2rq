/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq.map;

import java.sql.*;
import java.util.*;

import de.fuberlin.wiwiss.d2rq.helpers.Logger;

/**
 * Representation of a d2rq:Database from the mapping file.
 * It contains the connection information for the database and the column types of all database columns used.
 *
 * <p>History:<br>
 * 06-03-2004: Initial version of this class.<br>
 * 08-03-2004: Added some column type logic.<br>
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class Database {

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
    
    public static final int invalidColumnType=-1;
    public static final int noColumnType = 0;
    public static final int numericColumnType = 1;
    public static final int textColumnType = 2;
    public static final int dateColumnType = 3;
    public static final Integer numericColumn = new Integer(1);
    public static final Integer textColumn = new Integer(2);
    public static final Integer dateColumn = new Integer(3);


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

    public int getColumnType(String qualifiedColumnName) {
        Integer t=(Integer)this.columnTypes.get(qualifiedColumnName);
        if (t==null)
            return Database.noColumnType;
		return t.intValue();
    }
    
    /**
     * Returns the columnType for a given database column.
     * @return Node columnType D2RQ.textColumn or D2RQ.numericColumn or D2RQ.dateColumn
     */
    public int getColumnType(Column column) {
        return getColumnType(column.getQualifiedName());
	}

    /**
     * Raises a D2RQ error if there's no type information for the column.
     * @param column a database column
     */
	public void assertHasType(Column column) {
		if (getColumnType(column) == Database.noColumnType) {
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
    
    public  boolean databaseMayUseDistict=false;

   public void setAllowDistinct(boolean b) {
        databaseMayUseDistict=b;
    }

    /** 
     * Some Databases do not handle large entries correctly.
     * For example MSAccess cuts strings larger than 256 bytes when queried
     * with the DISTINCT keyword.
     * TODO We would need some assertions about a database or specific columns.
     * @return databaseMayUseDistict 
     */
    public boolean correctlyHandlesDistinct() {
        return databaseMayUseDistict;
    }
    
    private String expressionTranslator=null; // class name, if given
 
    public void setExpressionTranslator(String expressionTranslator) {
        this.expressionTranslator=expressionTranslator;
    }
    public String getExpressionTranslator() {
        return this.expressionTranslator;
    }

    public String toString() {
    	  return super.toString() + "(" + 
		  (odbc!=null ? odbc : "") + 
		  (jdbc!=null ? jdbc : "") +
		   ")";
    }


}
