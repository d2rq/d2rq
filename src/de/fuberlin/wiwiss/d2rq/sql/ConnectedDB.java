package de.fuberlin.wiwiss.d2rq.sql;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.rdql.ConstraintHandler;
import de.fuberlin.wiwiss.d2rq.rdql.ExpressionTranslator;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ConnectedDB.java,v 1.2 2006/09/13 14:18:16 cyganiak Exp $
 */
public class ConnectedDB {
	public static final String MySQL = "MySQL";
	public static final String Other = "Other";
	public static final int TEXT_COLUMN = 1;
	public static final int NUMERIC_COLUMN = 2;
	public static final int DATE_COLUMN = 3;
	
	private String jdbcURL;
	private String username;
	private String password;
	private String expressionTranslator;
	private boolean allowDistinct;
	private Set textColumns;
	private Set numericColumns;
	private Set dateColumns;
	private Connection connection = null;
	private DatabaseSchemaInspector schemaInspector = null;
	private String dbType = null;
	
	public ConnectedDB(String jdbcURL, String username, String password) {
		this(jdbcURL, username, password, null, true,
				Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET);
	}
	
	public ConnectedDB(String jdbcURL, String username, String password, String expressionTranslator,
			boolean allowDistinct, Set textColumns, Set numericColumns, Set dateColumns) {
		this.jdbcURL = jdbcURL;
		this.expressionTranslator = expressionTranslator;
		this.allowDistinct = allowDistinct;
		this.username = username;
		this.password = password;
		this.textColumns = textColumns;
		this.numericColumns = numericColumns;
		this.dateColumns = dateColumns;
	}
	
	public Connection connection() {
		if (this.connection == null) {
			this.connection = connect();
		}
		return this.connection;
	}
	
	private Connection connect() {
		try {
			return DriverManager.getConnection(this.jdbcURL, this.username, this.password);
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
	
	public DatabaseSchemaInspector schemaInspector() {
		if (this.schemaInspector == null) {
			this.schemaInspector = new DatabaseSchemaInspector(connection());
		}
		return this.schemaInspector;
	}

	/**
     * Reports the brand of RDBMS.
     * Will currently report one of these constants:
     * 
     * <ul>
     * <li><tt>DBConnection.MySQL</tt></li>
     * <li><tt>DBConnection.Other</tt></li>
     * </ul>
     * @return The brand of RDBMS
     */
	public String dbType() {
		if (this.dbType == null) {
			try {
				if (connection().getMetaData().getDriverName().toLowerCase().indexOf("mysql") >= 0) {
					this.dbType = ConnectedDB.MySQL;
				} else {
					this.dbType = ConnectedDB.Other;
				}
			} catch (SQLException ex) {
				throw new D2RQException("Database exception", ex);
			}
		}
		return this.dbType;
	}
	
	public boolean dbTypeIs(String candidateType) {
		return candidateType.equals(dbType());
	}
	
    public int columnType(String qualifiedColumnName) {
		return columnType(new Attribute(qualifiedColumnName));
    }
    
    /**
     * Returns the columnType for a given database column.
     * @return Node columnType D2RQ.textColumn or D2RQ.numericColumn or D2RQ.dateColumn
     */
    public int columnType(Attribute column) {
    	if (this.textColumns.contains(column.qualifiedName())) {
    		return TEXT_COLUMN;
    	}
    	if (this.numericColumns.contains(column.qualifiedName())) {
    		return NUMERIC_COLUMN;
    	}
    	if (this.dateColumns.contains(column.qualifiedName())) {
    		return DATE_COLUMN;
    	}
		int type = schemaInspector().columnType(column);
		switch (type) {
			// TODO There are a bunch of others, see http://java.sun.com/j2se/1.5.0/docs/api/java/sql/Types.html
			case Types.CHAR: return TEXT_COLUMN;
			case Types.VARCHAR: return TEXT_COLUMN;
			case Types.LONGVARCHAR: return TEXT_COLUMN;
			case Types.NUMERIC: return NUMERIC_COLUMN;
			case Types.DECIMAL: return NUMERIC_COLUMN;
			case Types.BIT: return NUMERIC_COLUMN;
			case Types.TINYINT: return NUMERIC_COLUMN;
			case Types.SMALLINT: return NUMERIC_COLUMN;
			case Types.INTEGER: return NUMERIC_COLUMN;
			case Types.BIGINT: return NUMERIC_COLUMN;
			case Types.REAL: return NUMERIC_COLUMN;
			case Types.FLOAT: return NUMERIC_COLUMN;
			case Types.DOUBLE: return NUMERIC_COLUMN;
	
			// TODO: What to do with binary columns?
			case Types.BINARY: return TEXT_COLUMN;
			case Types.VARBINARY: return TEXT_COLUMN;
			case Types.LONGVARBINARY: return TEXT_COLUMN;
	
			case Types.DATE: return DATE_COLUMN;
			case Types.TIME: return DATE_COLUMN;
			case Types.TIMESTAMP: return DATE_COLUMN;
			
			default: throw new D2RQException("Unsupported database type code (" + type + ") for column "
					+ column.qualifiedName());
		}
	}

	// TODO Better if we didn't have to pass these parameters in here, and use a non-arg constructor
	public ExpressionTranslator expressionTranslator(ConstraintHandler handler, SelectStatementBuilder sql) {
		if (this.expressionTranslator == null) {
			return new ExpressionTranslator(handler, sql);
		} else {
			try {
				Class c = Class.forName(this.expressionTranslator);
				Constructor constructor = c.getConstructor(
						new Class[]{ConstraintHandler.class, SelectStatementBuilder.class});
				return (ExpressionTranslator) constructor.newInstance(new Object[]{this,sql});
		    } catch (ClassNotFoundException ex) {
		    	throw new D2RQException("Couldn't find d2rq:expressionTranslator class " + this.expressionTranslator);
		    } catch (Exception ex) {
		    	throw new D2RQException("Couldn't instantiate " + this.expressionTranslator, ex);
		    }
		}
	}
	
	/** 
	 * Some Databases do not handle large entries correctly.
	 * For example MSAccess cuts strings larger than 256 bytes when queried
	 * with the DISTINCT keyword.
	 * TODO We would need some assertions about a database or specific columns.
	 */
	public boolean allowDistinct() {
		return this.allowDistinct;
	}

	public void close() {
		if (this.connection == null) {
			return;
		}
		try {
			this.connection.close();
		} catch (SQLException ex) {
			throw new D2RQException(ex);
		}
	}
	
    public boolean equals(Object otherObject) {
    	if (!(otherObject instanceof ConnectedDB)) {
    		return false;
    	}
    	ConnectedDB other = (ConnectedDB) otherObject;
    	return this.jdbcURL.equals(other.jdbcURL);
    }
    
    public int hashCode() {
    	return this.jdbcURL.hashCode();
    }
}