package de.fuberlin.wiwiss.d2rq.map;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * Inspects a database to retrieve schema information. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DatabaseSchemaInspector.java,v 1.1 2006/05/20 14:03:13 cyganiak Exp $
 */
public class DatabaseSchemaInspector {
	private DatabaseMetaData schema;
	
	public DatabaseSchemaInspector(Connection connection) {
		try {
			this.schema = connection.getMetaData();
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public int columnType(Column column) {
		try {
			ResultSet rs = this.schema.getColumns(null, null, column.getTableName(), column.getColumnName());
			if (!rs.next()) {
				throw new D2RQException("Column " + column + " not found in database");
			}
			return rs.getInt("DATA_TYPE");
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
}
