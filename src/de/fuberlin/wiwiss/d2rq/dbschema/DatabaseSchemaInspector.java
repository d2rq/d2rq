package de.fuberlin.wiwiss.d2rq.dbschema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;

/**
 * Inspects a database to retrieve schema information. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DatabaseSchemaInspector.java,v 1.1 2006/09/11 23:22:27 cyganiak Exp $
 */
public class DatabaseSchemaInspector {
	
	public static boolean isStringType(int columnType) {
		return columnType == Types.CHAR || columnType == Types.VARCHAR || columnType == Types.LONGVARCHAR;
	}

	public static boolean isDateType(int columnType) {
		return columnType == Types.DATE || columnType == Types.TIME || columnType == Types.TIMESTAMP;
	}
	
	public static String xsdTypeFor(int columnType) {
		switch (columnType) {
		case Types.BIGINT:        return "xsd:long";
//		case Types.BINARY:        return "xsd:hexBinary";
//		case Types.BIT:           return "xsd:boolean";
//		case Types.BLOB:          return "xsd:hexBinary";
		case Types.CHAR:          return "xsd:string";
//		case Types.CLOB:          return "xsd:string";
		case Types.DATE:          return "xsd:date";
		case Types.DECIMAL:       return "xsd:decimal";
		case Types.DOUBLE:        return "xsd:double";
		case Types.FLOAT:         return "xsd:decimal";
		case Types.INTEGER:       return "xsd:int";
//		case Types.JAVA_OBJECT:   return "xsd:string";
//		case Types.LONGVARBINARY: return "xsd:hexBinary";
		case Types.LONGVARCHAR:   return "xsd:string";
		case Types.NUMERIC:       return "xsd:decimal";
		case Types.REAL:          return "xsd:float";
//		case Types.REF:           return "xsd:IDREF";
		case Types.SMALLINT:      return "xsd:short";
//		case Types.STRUCT:        return "struct";
//		case Types.TIME:          return "xsd:time";
		case Types.TIMESTAMP:     return "xsd:dateTime";
		case Types.TINYINT:       return "xsd:byte";
//		case Types.VARBINARY:     return "xsd:hexBinary";
		case Types.VARCHAR:       return "xsd:string";
		default: return null;
		}
	}
	
	private DatabaseMetaData schema;
	
	public DatabaseSchemaInspector(Connection connection) {
		try {
			this.schema = connection.getMetaData();
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public int columnType(Attribute column) {
		try {
			ResultSet rs = this.schema.getColumns(null, null, column.tableName(), column.attributeName());
			if (!rs.next()) {
				throw new D2RQException("Column " + column + " not found in database");
			}
			return rs.getInt("DATA_TYPE");
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List listTableNames() {
		List result = new ArrayList();
		try {
			ResultSet rs = this.schema.getTables(null, null, null, null);
			while (rs.next()) {
				result.add(rs.getString("TABLE_NAME"));
			}
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List listColumns(String tableName) {
		List result = new ArrayList();
		try {
			ResultSet rs = this.schema.getColumns(null, null, tableName, null);
			while (rs.next()) {
				result.add(new Attribute(tableName, rs.getString("COLUMN_NAME")));
			}
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List primaryKeyColumns(String tableName) {
		List result = new ArrayList();
		try {
			ResultSet rs = this.schema.getPrimaryKeys(null, null, tableName);
			while (rs.next()) {
				result.add(new Attribute(tableName, rs.getString("COLUMN_NAME")));
			}
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List foreignKeyColumns(String tableName) {
		try {
			List result = new ArrayList();
			ResultSet rs = this.schema.getImportedKeys(null, null, tableName);
			while (rs.next()) {
				String pkTableName = rs.getString("PKTABLE_NAME");
				String pkColumnName = rs.getString("PKCOLUMN_NAME");
				Attribute primaryColumn = new Attribute(pkTableName, pkColumnName);
				String fkTableName = rs.getString("FKTABLE_NAME");
				String fkColumnName = rs.getString("FKCOLUMN_NAME");
				Attribute foreignColumn = new Attribute(fkTableName, fkColumnName);
				result.add(new Attribute[]{foreignColumn, primaryColumn});
			}
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}

	public boolean isLinkTable(String tableName) {
		if (listColumns(tableName).size() != 2) {
			return false;
		}
		return foreignKeyColumns(tableName).size() == 2;
	}
}
