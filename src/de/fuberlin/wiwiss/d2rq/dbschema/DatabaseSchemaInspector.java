package de.fuberlin.wiwiss.d2rq.dbschema;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * Inspects a database to retrieve schema information. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DatabaseSchemaInspector.java,v 1.9 2007/11/15 15:29:32 cyganiak Exp $
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
	
	private ConnectedDB db;
	private DatabaseMetaData schema;
	private Map cachedColumnTypes = new HashMap();
	private Map cachedColumnNullability = new HashMap();
	
	public DatabaseSchemaInspector(ConnectedDB db) {
		try {
			this.db = db;
			this.schema = db.connection().getMetaData();
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public int columnType(Attribute column) {
		if (this.cachedColumnTypes.containsKey(column)) {
			return ((Integer) this.cachedColumnTypes.get(column)).intValue();
		}
		try {
			ResultSet rs = this.schema.getColumns(null, column.schemaName(), 
					column.tableName(), column.attributeName());
			if (!rs.next()) {
				throw new D2RQException("Column " + column + " not found in database");
			}
			int type = rs.getInt("DATA_TYPE");
			rs.close();
			this.cachedColumnTypes.put(column, new Integer(type));
			return type;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public boolean isNullable(Attribute column) {
		if (this.cachedColumnNullability.containsKey(column)) {
			return ((Boolean) this.cachedColumnNullability.get(column)).booleanValue();
		}
		try {
			ResultSet rs = this.schema.getColumns(null, column.schemaName(), 
					column.tableName(), column.attributeName());
			if (!rs.next()) {
				throw new D2RQException("Column " + column + " not found in database");
			}
			boolean nullable = (rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
			rs.close();
			this.cachedColumnNullability.put(column, new Boolean(nullable));
			return nullable;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public boolean isZerofillColumn(Attribute column) {
		try {
			if (!db.dbTypeIs(ConnectedDB.MySQL)) return false;
			Statement stmt = db.connection().createStatement();
			ResultSet rs = stmt.executeQuery("DESCRIBE " + db.quoteRelationName(column.relationName()));
			while (rs.next()) {
				if (!column.attributeName().equals(rs.getString("Field"))) continue;
				return rs.getString("Type").toLowerCase().indexOf("zerofill") != -1;
			}
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
		throw new D2RQException("Column not found in DESCRIBE result: " + column);
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
				result.add(toRelationName(schema, table));
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}

	public List listColumns(RelationName tableName) {
		List result = new ArrayList();
		try {
			ResultSet rs = this.schema.getColumns(
					null, schemaName(tableName), tableName(tableName), null);
			while (rs.next()) {
				result.add(new Attribute(tableName, rs.getString("COLUMN_NAME")));
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List primaryKeyColumns(RelationName tableName) {
		List result = new ArrayList();
		try {
			ResultSet rs = this.schema.getPrimaryKeys(
					null, schemaName(tableName), tableName(tableName));
			while (rs.next()) {
				result.add(new Attribute(tableName, rs.getString("COLUMN_NAME")));
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	public List foreignKeyColumns(RelationName tableName) {
		try {
			List result = new ArrayList();
			ResultSet rs = this.schema.getImportedKeys(
					null, schemaName(tableName), tableName(tableName));
			while (rs.next()) {
				RelationName pkTable = toRelationName(
						rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"));
				Attribute primaryColumn = new Attribute(pkTable, rs.getString("PKCOLUMN_NAME"));
				RelationName fkTable = toRelationName(
						rs.getString("FKTABLE_SCHEM"), rs.getString("FKTABLE_NAME"));
				Attribute foreignColumn = new Attribute(fkTable, rs.getString("FKCOLUMN_NAME"));
				result.add(new Attribute[]{foreignColumn, primaryColumn});
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}

	public boolean isLinkTable(RelationName tableName) {
		if (listColumns(tableName).size() != 2) {
			return false;
		}
		return foreignKeyColumns(tableName).size() == 2;
	}
	
	private String schemaName(RelationName tableName) {
		if (this.db.dbTypeIs(ConnectedDB.PostgreSQL) && tableName.schemaName() == null) {
			// The default schema is known as "public" in PostgreSQL 
			return "public";
		}
		return tableName.schemaName();
	}
	
	private String tableName(RelationName tableName) {
		return tableName.tableName();
	}

	private RelationName toRelationName(String schema, String table) {
		if (schema == null) {
			// Table without schema
			return new RelationName(null, table);
		} else if (this.db.dbTypeIs(ConnectedDB.PostgreSQL) && "public".equals(schema)) {
			// Table in PostgreSQL default schema -- call the table "foo", not "public.foo"
			return new RelationName(null, table);
		}
		return new RelationName(schema, table);
	}
}
