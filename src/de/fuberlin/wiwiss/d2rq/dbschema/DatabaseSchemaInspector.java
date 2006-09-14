package de.fuberlin.wiwiss.d2rq.dbschema;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * Inspects a database to retrieve schema information. 
 * 
 * TODO: Input and output RelationNames instead of Strings
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DatabaseSchemaInspector.java,v 1.4 2006/09/14 16:22:48 cyganiak Exp $
 */
public class DatabaseSchemaInspector {
	private static Pattern schemaAndTableRegex = Pattern.compile("(?:(.*)\\.)?(.*?)");
	
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
	
	private ConnectedDB db;
	private DatabaseMetaData schema;
	
	public DatabaseSchemaInspector(ConnectedDB db) {
		try {
			this.db = db;
			this.schema = db.connection().getMetaData();
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public int columnType(Attribute column) {
		try {
			ResultSet rs = this.schema.getColumns(null, column.schemaName(), 
					column.tableName(), column.attributeName());
			if (!rs.next()) {
				throw new D2RQException("Column " + column + " not found in database");
			}
			int type = rs.getInt("DATA_TYPE");
			rs.close();
			return type;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List listTableNames() {
		List result = new ArrayList();
		try {
			String searchInSchema = null;
			if (this.db.dbTypeIs(ConnectedDB.Oracle)) {
				searchInSchema = this.schema.getUserName();
			}
			ResultSet rs = this.schema.getTables(
					null, searchInSchema, null, new String[] {"TABLE", "VIEW"});
			while (rs.next()) {
				String schema = rs.getString("TABLE_SCHEM");
				String table = rs.getString("TABLE_NAME");
				if (this.db.dbTypeIs(ConnectedDB.PostgreSQL) 
						&& ("information_schema".equals(schema)
								|| "pg_catalog".equals(schema))) {
					// PostgreSQL has schemas "information_schema" and "pg_catalog" in every DB
					continue;
				}
				if (this.db.dbTypeIs(ConnectedDB.Oracle)
						&& table.startsWith("BIN$")) {
					// Skip deleted tables in Oracle's Recycling Bin.
					// They have names like MYSCHEMA.BIN$FoHqtx6aQ4mBaMQmlTCPTQ==$0
					continue;
				}
				result.add(createQualifiedTableName(schema, table));
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}

	public List listColumns(String qualifiedTableName) {
		List result = new ArrayList();
		try {
			ResultSet rs = this.schema.getColumns(
					null, schemaName(qualifiedTableName), tableName(qualifiedTableName), null);
			while (rs.next()) {
				result.add(new Attribute(qualifiedTableName + "." + rs.getString("COLUMN_NAME")));
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List primaryKeyColumns(String qualifiedTableName) {
		List result = new ArrayList();
		try {
			ResultSet rs = this.schema.getPrimaryKeys(
					null, schemaName(qualifiedTableName), tableName(qualifiedTableName));
			while (rs.next()) {
				result.add(new Attribute(qualifiedTableName + "." + rs.getString("COLUMN_NAME")));
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List foreignKeyColumns(String qualifiedTableName) {
		try {
			List result = new ArrayList();
			ResultSet rs = this.schema.getImportedKeys(
					null, schemaName(qualifiedTableName), tableName(qualifiedTableName));
			while (rs.next()) {
				String pkTableName = createQualifiedTableName(
						rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"));
				String pkColumnName = rs.getString("PKCOLUMN_NAME");
				Attribute primaryColumn = new Attribute(pkTableName + "." + pkColumnName);
				String fkTableName = createQualifiedTableName(
						rs.getString("FKTABLE_SCHEM"), rs.getString("FKTABLE_NAME"));
				String fkColumnName = rs.getString("FKCOLUMN_NAME");
				Attribute foreignColumn = new Attribute(fkTableName + "." + fkColumnName);
				result.add(new Attribute[]{foreignColumn, primaryColumn});
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}

	public boolean isLinkTable(String qualifiedTableName) {
		if (listColumns(qualifiedTableName).size() != 2) {
			return false;
		}
		return foreignKeyColumns(qualifiedTableName).size() == 2;
	}
	
	private String schemaName(String qualifiedTableName) {
		Matcher match = schemaAndTableRegex.matcher(qualifiedTableName);
		match.matches();
		if (this.db.dbTypeIs(ConnectedDB.PostgreSQL) && match.group(1) == null) {
			// The default schema is known as "public" in PostgreSQL 
			return "public";
		}
		return match.group(1);
	}
	
	private String tableName(String qualifiedTableName) {
		Matcher match = schemaAndTableRegex.matcher(qualifiedTableName);
		match.matches();
		return match.group(2);
	}

	private String createQualifiedTableName(String schema, String table) {
		if (schema == null) {
			// Table without schema
			return table;
		} else if (this.db.dbTypeIs(ConnectedDB.PostgreSQL) && "public".equals(schema)) {
			// Table in PostgreSQL default schema -- call the table "foo", not "public.foo"
			return table;
		}
		return schema + "." + table;
	}
}
