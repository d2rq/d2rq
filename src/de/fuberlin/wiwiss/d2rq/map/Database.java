package de.fuberlin.wiwiss.d2rq.map;


import java.sql.*;
import java.util.*;

import de.fuberlin.wiwiss.d2rq.D2RQException;
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
 * 
 * TODO: Make a bunch of public methods private?
 */
public class Database {

	/**
	 * Pre-registers a JDBC driver if its class can be found on the
	 * classpath. If the class is not found, nothing will happen.
	 * @param driverClassName Fully qualified class name of a JDBC driver
	 */
	public static void registerJDBCDriverIfPresent(String driverClassName) {
		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException ex) {
			// not present, just ignore this driver
		}
	}
	
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

	private DatabaseSchemaInspector schemaInspector = null;
	private String rdbmsType = null;

    public Database(String odbc, String jdbc, String jdbcDriver, String databaseUsername, String databasePassword, Map columnTypes) {
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
    		return getColumnType(new Column(qualifiedColumnName));
    }
    
    /**
     * Returns the columnType for a given database column.
     * @return Node columnType D2RQ.textColumn or D2RQ.numericColumn or D2RQ.dateColumn
     */
    public int getColumnType(Column column) {
		Integer t = (Integer) this.columnTypes.get(column.getQualifiedName());
		if (t != null) {
			return t.intValue();
		}
		int type = schemaInspector().columnType(column);
		switch (type) {
		// TODO There are a bunch of others, see http://java.sun.com/j2se/1.5.0/docs/api/java/sql/Types.html
		case Types.CHAR: return Database.textColumnType;
		case Types.VARCHAR: return Database.textColumnType;
		case Types.LONGVARCHAR: return Database.textColumnType;
		case Types.NUMERIC: return Database.numericColumnType;
		case Types.DECIMAL: return Database.numericColumnType;
		case Types.BIT: return Database.numericColumnType;
		case Types.TINYINT: return Database.numericColumnType;
		case Types.SMALLINT: return Database.numericColumnType;
		case Types.INTEGER: return Database.numericColumnType;
		case Types.BIGINT: return Database.numericColumnType;
		case Types.REAL: return Database.numericColumnType;
		case Types.FLOAT: return Database.numericColumnType;
		case Types.DOUBLE: return Database.numericColumnType;

		// TODO: What to do with binary columns?
		case Types.BINARY: return Database.textColumnType;
		case Types.VARBINARY: return Database.textColumnType;
		case Types.LONGVARBINARY: return Database.textColumnType;

		case Types.DATE: return Database.dateColumnType;
		case Types.TIME: return Database.dateColumnType;
		case Types.TIMESTAMP: return Database.dateColumnType;
		
		default: throw new D2RQException("Unsupported database type code (" + type + ") for column "
				+ column.getQualifiedName());
		}
	}

	private DatabaseSchemaInspector schemaInspector() {
		if (this.schemaInspector == null) {
			this.schemaInspector = new DatabaseSchemaInspector(getConnnection());
		}
		return this.schemaInspector;
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
            if (this.odbc != null) {
                this.jdbc = "jdbc:odbc:" + this.odbc;
                this.jdbcDriver = "sun.jdbc.odbc.JdbcOdbcDriver";
            }
            if (this.jdbc == null) {
                Logger.instance().error("Could not connect to database because of missing URL.");
                return;
            }
            if (this.jdbcDriver != null) {
                Class.forName(this.jdbcDriver);
            } else {
                this.jdbcDriver = DriverManager.getDriver(this.jdbc).getClass().getName();
            }
            if (this.getDatabaseUsername() != null || this.getDatabasePassword() != null) {
            		this.con = DriverManager.getConnection(this.jdbc, this.getDatabaseUsername(), this.getDatabasePassword());
            } else {
            		this.con = DriverManager.getConnection(this.jdbc);
            }
            this.connectedToDatabase = true;
        } catch (SQLException ex) {
            throw new D2RQException(ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            throw new D2RQException("Database driver class not found in the lib directory: " + ex.getMessage(), ex);
        }
    }
    
   public boolean databaseMayUseDistinct=true;

   public void setAllowDistinct(boolean b) {
        databaseMayUseDistinct=b;
    }

    /** 
     * Some Databases do not handle large entries correctly.
     * For example MSAccess cuts strings larger than 256 bytes when queried
     * with the DISTINCT keyword.
     * TODO We would need some assertions about a database or specific columns.
     * @return databaseMayUseDistinct 
     */
    public boolean correctlyHandlesDistinct() {
        return databaseMayUseDistinct;
    }
    
    private String expressionTranslator=null; // class name, if given
 
    public void setExpressionTranslator(String expressionTranslator) {
        this.expressionTranslator=expressionTranslator;
    }
    public String getExpressionTranslator() {
        return this.expressionTranslator;
    }

    /**
     * Connects to the database and reports the brand of RDBMS.
     * Will currently report one of these:
     * 
     * <ul>
     * <li>MySQL</li>
     * <li>Other</li>
     * </ul>
     * @return The brand of RDBMS
     */
	public String getType() {
		if (this.rdbmsType == null) {
			connectToDatabase();
			try {
				if (this.con.getMetaData().getDriverName().toLowerCase().indexOf("mysql") >= 0) {
					this.rdbmsType = "MySQL";
				} else {
					this.rdbmsType = "Other";
				}
			} catch (SQLException ex) {
				throw new D2RQException("Database exception", ex);
			}
		}
		return this.rdbmsType;
	}
    
    public String toString() {
    	  return super.toString() + "(" + 
		  (odbc!=null ? odbc : "") + 
		  (jdbc!=null ? jdbc : "") +
		   ")";
    }
}