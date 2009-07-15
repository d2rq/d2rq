package de.fuberlin.wiwiss.d2rq.dbschema;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * Inspects a database to retrieve schema information. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DatabaseSchemaInspector.java,v 1.16 2009/07/15 12:14:01 fatorange Exp $
 */
public class DatabaseSchemaInspector {
	
	public static boolean isStringType(int columnType) {
		return columnType == Types.CHAR || columnType == Types.VARCHAR || columnType == ConnectedDB.SQL_TYPE_NVARCHAR || columnType == Types.LONGVARCHAR;
	}

	public static boolean isDateType(int columnType) {
		return columnType == Types.DATE || columnType == Types.TIME || columnType == Types.TIMESTAMP;
	}
	
	public static String xsdTypeFor(int columnType) {
		switch (columnType) {
		case Types.BIGINT:        return "xsd:long";
//		case Types.BINARY:        return "xsd:hexBinary";
		// TODO: BIT is a bit string, not a single boolean
		// But the MySQL JDBC driver reports TINYINT(1) as BIT 
		case Types.BIT:           return "xsd:boolean";
//		case Types.BLOB:          return "xsd:hexBinary";
		case Types.BOOLEAN:       return "xsd:boolean";
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
		case ConnectedDB.SQL_TYPE_NVARCHAR:   return "xsd:string";
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
	
	public static final int KEYS_IMPORTED = 0;
	public static final int KEYS_EXPORTED = 1;
	
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
		boolean isZerofill = false;
		boolean foundColumn = false;
		
		try {
			if (!db.dbTypeIs(ConnectedDB.MySQL)) return false;
			Statement stmt = db.connection().createStatement();
			ResultSet rs = stmt.executeQuery("DESCRIBE " + db.quoteRelationName(column.relationName()));		

			while (rs.next()) {
				if (column.attributeName().equals(rs.getString("Field"))) {
					isZerofill = (rs.getString("Type").toLowerCase().indexOf("zerofill") != -1);
					foundColumn = true;
					break;
				}
			}
			
			rs.close();
			stmt.close();			

			if (foundColumn)
				return isZerofill;	
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
	
	/**
	 * Returns a list of imported or exported (foreign) keys for a table.
	 * @param tableName The table we are interested in
	 * @param direction If set to {@link KEYS_IMPORTED}, the table's foreign keys are returned.
	 * 					If set to {@link KEYS_EXPORTED}, the table's primary keys referenced from other tables are returned.
	 * @return A list of {@link Join}s; the local columns are in attributes1() 
	 */
	public List foreignKeys(RelationName tableName, int direction) {
		try {
			Map fks = new HashMap();
			ResultSet rs = (direction == KEYS_IMPORTED ? this.schema.getImportedKeys(null, schemaName(tableName), tableName(tableName))
													   : this.schema.getExportedKeys(null, schemaName(tableName), tableName(tableName)));
			while (rs.next()) {
				RelationName pkTable = toRelationName(
						rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"));
				Attribute primaryColumn = new Attribute(pkTable, rs.getString("PKCOLUMN_NAME"));
				RelationName fkTable = toRelationName(
						rs.getString("FKTABLE_SCHEM"), rs.getString("FKTABLE_NAME"));
				Attribute foreignColumn = new Attribute(fkTable, rs.getString("FKCOLUMN_NAME"));
				String fkName = rs.getString("FK_NAME");
				if (!fks.containsKey(fkName)) {
					fks.put(fkName, new ForeignKey());
				}
				int keySeq = rs.getInt("KEY_SEQ") - 1;
				((ForeignKey) fks.get(fkName)).addColumns(
						keySeq, foreignColumn, primaryColumn);
			}
			rs.close();
			List results = new ArrayList();
			Iterator it = fks.values().iterator();
			while (it.hasNext()) {
				ForeignKey fk = (ForeignKey) it.next();
				results.add(fk.toJoin());
			}
			return results;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}	

	/**
	 * A table T is considered to be a link table if it has exactly two
	 * foreign key constraints, and the constraints reference other
	 * tables (not T), and the constraints cover all columns of T.
	 * 
	 * TODO: Should check that the table is not referenced by foreign keys from other tables
	 */
	public boolean isLinkTable(RelationName tableName) {
		List foreignKeys = foreignKeys(tableName, KEYS_IMPORTED);
		if (foreignKeys.size() != 2) return false;
		
		List exportedKeys = foreignKeys(tableName, KEYS_EXPORTED);
		if (!exportedKeys.isEmpty()) return false;
		
		List columns = listColumns(tableName);
		Iterator it = foreignKeys.iterator();
		while (it.hasNext()) {
			Join fk = (Join) it.next();
			if (fk.isSameTable()) return false;
			columns.removeAll(fk.attributes1());
		}
		return columns.isEmpty();
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
			return new RelationName(null, table, db.lowerCaseTableNames());
		} else if (this.db.dbTypeIs(ConnectedDB.PostgreSQL) && "public".equals(schema)) {
			// Table in PostgreSQL default schema -- call the table "foo", not "public.foo"
			return new RelationName(null, table, db.lowerCaseTableNames());
		}
		return new RelationName(schema, table, db.lowerCaseTableNames());
	}

	/**
	 * A foreign key. Supports adding (local column, other column) pairs. The pairs
	 * can be added out of order and will be re-ordered internally. When all
	 * columns are added, a {@link Join} object can be created.
	 */
	private class ForeignKey {
		private TreeMap primaryColumns = new TreeMap();
		private TreeMap foreignColumns = new TreeMap();
		private void addColumns(int keySequence, Attribute foreign, Attribute primary) {
			primaryColumns.put(new Integer(keySequence), primary);
			foreignColumns.put(new Integer(keySequence), foreign);
		}
		private Join toJoin() {
			return new Join(
					new ArrayList(foreignColumns.values()),
					new ArrayList(primaryColumns.values()));
		}
	}

	/**
	 * Looks up a RelationName with the schema in order to retrieve the correct capitalization
	 * 
	 * @param tableName
	 * @return
	 */
	public RelationName getCorrectCapitalization(RelationName relationName) {
		if (!relationName.caseUnspecified() || !db.lowerCaseTableNames())
			return relationName;
		
		List tables = listTableNames();
		Iterator it = tables.iterator();
		while (it.hasNext()) {
			RelationName r = (RelationName) it.next();
			if (r.equals(relationName))
				return r;
		}
		return null;
	}
}
