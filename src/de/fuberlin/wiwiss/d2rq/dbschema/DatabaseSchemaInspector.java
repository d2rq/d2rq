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
import de.fuberlin.wiwiss.d2rq.sql.SQLSyntax;

/**
 * Inspects a database to retrieve schema information. 
 * 
 * TODO: All the dbType checks should be moved to the {@link SQLSyntax} subclasses
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class DatabaseSchemaInspector {
	
	public static boolean isStringType(ColumnType columnType) {
		return columnType.typeId() == Types.CHAR || columnType.typeId() == Types.VARCHAR || columnType.typeId() == Types.NVARCHAR
					|| columnType.typeId() == Types.LONGVARCHAR	|| "NVARCHAR2".equals(columnType.typeName());
	}

	public static boolean isDateType(ColumnType columnType) {
		return columnType.typeId() == Types.DATE || columnType.typeId() == Types.TIME || columnType.typeId() == Types.TIMESTAMP;
	}
	
	/**
	 * Return the appropriate XSD datatype for a SQL column type. <code>null</code>
	 * indicates an unsupported SQL type. {@link ColumnType#UNMAPPABLE} indicates
	 * an unmappable SQL type.
	 * 
	 * TODO: The MySQL JDBC driver reports TINYINT(1) as BIT, should be handled as xsd:boolean
 	 * 
	 * @param columnType
	 * @return XSD datatype as prefixed name: <code>xsd:string</code> etc.
	 */
	public String xsdTypeFor(ColumnType columnType) {
		if (columnType.typeId() == Types.OTHER && db.dbTypeIs(ConnectedDB.HSQLDB)) {
			// OTHER in HSQLDB 2.8.8 is really JAVA_OBJECT
			return ColumnType.UNMAPPABLE;
		}
		
		// HACK: MS SQLServer 2008 returns 'date' as VARCHAR type
		if(columnType.typeName().equals("date") && db.dbTypeIs(ConnectedDB.MSSQL)) {
			return "xsd:date";
		}
		
// HACK: MS SQLServer 2008 returns 'datetime2(7)' and 'datetimeoffset(7)' as VARCHAR type
// TODO: Cant make it work. See comment in ResultRowMap.java for additional information on datatype 
// inconsistency particularly in the case of MS SQLServer.
//		if((columnType.typeName().equals("datetime2") && db.dbTypeIs(ConnectedDB.MSSQL)) || (columnType.typeName().equals("datetimeoffset") && db.dbTypeIs(ConnectedDB.MSSQL))) {
//			return "xsd:dateTime";
//		}
		
		if (db.dbTypeIs(ConnectedDB.MySQL) && columnType.typeName().contains("UNSIGNED")) {
			switch (columnType.typeId()) {
			case Types.TINYINT: return "xsd:unsignedByte";
			case Types.SMALLINT: return "xsd:unsignedShort";
			case Types.INTEGER: return "xsd:unsignedInt";
			case Types.BIGINT: return "xsd:unsignedLong";
			}
		}
		if (db.dbTypeIs(ConnectedDB.MySQL) && columnType.typeId() == Types.BIT
				&& columnType.size() == 0) {
			// MySQL reports TINYINT(1) as BIT, but all other BITs as BIT(M).
			// This is conventionally treated as BOOLEAN.
			return "xsd:boolean";
		}

		switch (columnType.typeId()) {
		case Types.ARRAY:         return ColumnType.UNMAPPABLE;
		case Types.BIGINT:        return "xsd:long";
		case Types.BINARY:        return "xsd:hexBinary";
		case Types.BIT:           return "xsd:string";
		case Types.BLOB:          return "xsd:hexBinary";
		case Types.BOOLEAN:       return "xsd:boolean";
		case Types.CHAR:          return "xsd:string";
		case Types.CLOB:          return "xsd:string";
		case Types.DATALINK:      return null;
		case Types.DATE:          return "xsd:date";
		case Types.DECIMAL:       return "xsd:decimal";
		case Types.DISTINCT:      return null;
		case Types.DOUBLE:        return "xsd:double";
		case Types.FLOAT:         return "xsd:double";
		case Types.INTEGER:       return "xsd:int";
		case Types.JAVA_OBJECT:   return ColumnType.UNMAPPABLE;
		case Types.LONGVARBINARY: return "xsd:hexBinary";
		case Types.LONGVARCHAR:   return "xsd:string";
		case Types.NULL:          return null;
		case Types.NUMERIC:       return "xsd:decimal";
		case Types.OTHER:         return null;
		case Types.REAL:          return "xsd:double";
		case Types.REF:           return null;
		case Types.SMALLINT:      return "xsd:short";
		case Types.STRUCT:        return null;
		case Types.TIME:          return "xsd:time";
		case Types.TIMESTAMP:     return "xsd:dateTime";
		case Types.TINYINT:       return "xsd:byte";
		case Types.VARBINARY:     return "xsd:hexBinary";
		case Types.VARCHAR:       return "xsd:string";
		}
		if ("NVARCHAR2".equals(columnType.typeName())) {
			return "xsd:string";
		}
		if ("NVARCHAR".equals(columnType.typeName())) {
			return "xsd:string";
		}
		if ("BINARY_DOUBLE".equals(columnType.typeName())) {
			return "xsd:double";
		}
		if ("BINARY_FLOAT".equals(columnType.typeName())) {
			return "xsd:double";
		}
		if ("BFILE".equals(columnType.typeName())) {
			return ColumnType.UNMAPPABLE;
		}
		return null;
	}
	
	private ConnectedDB db;
	private DatabaseMetaData schema;
	private Map<Attribute,ColumnType> cachedColumnTypes = 
			new HashMap<Attribute,ColumnType>();
	private Map<Attribute,Boolean> cachedColumnNullability = 
			new HashMap<Attribute,Boolean>();
	
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
	
	public ColumnType columnType(Attribute column) {
		if (this.cachedColumnTypes.containsKey(column)) {
			return (ColumnType) this.cachedColumnTypes.get(column);
		}
		try {
			ResultSet rs = this.schema.getColumns(null, column.schemaName(), 
					column.tableName(), column.attributeName());
			if (!rs.next()) {
				throw new D2RQException("Column " + column + " not found in database");
			}
			ColumnType type = new ColumnType(rs.getInt("DATA_TYPE"), 
					rs.getString("TYPE_NAME"), rs.getInt("COLUMN_SIZE"));
			rs.close();
			this.cachedColumnTypes.put(column, type);
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
			ResultSet rs = stmt.executeQuery("DESCRIBE " + db.getSyntax().quoteRelationName(column.relationName()));		

			while (rs.next()) {
				// MySQL names are case insensitive, so we normalize to lower case
				if (column.attributeName().toLowerCase().equals(rs.getString("Field").toLowerCase())) {
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

	/**
	 * Lists available table names
	 * @param searchInSchema	Schema to list tables from; <tt>null</tt> to list tables from all schemas
	 * @return A list of {@link RelationName}s
	 */
	public List<RelationName> listTableNames(String searchInSchema) {
		List<RelationName> result = new ArrayList<RelationName>();
		try {
			ResultSet rs = this.schema.getTables(
					null, searchInSchema, null, new String[] {"TABLE", "VIEW"});
			while (rs.next()) {
				String schema = rs.getString("TABLE_SCHEM");
				String table = rs.getString("TABLE_NAME");
				if (!this.db.isIgnoredTable(schema, table)) {
					result.add(toRelationName(schema, table));
				}
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}

	public List<Attribute> listColumns(RelationName tableName) {
		List<Attribute> result = new ArrayList<Attribute>();
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
	
	public List<Attribute> primaryKeyColumns(RelationName tableName) {
		List<Attribute> result = new ArrayList<Attribute>();
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
	 * Returns unique indexes defined on the table.
	 * @param tableName Name of a table
	 * @return Map from index name to list of column names
	 */
	public Map<String,List<String>> uniqueColumns(RelationName tableName) {
		Map<String,List<String>> result = new HashMap<String,List<String>>();
		try {
			/*
			 * When requesting index info from an Oracle database, accept approximate
			 * data, as requesting exact results will invoke an ANALYZE, for which the
			 * querying user must have proper write permissions.
			 * If he doesn't, an SQLException is thrown right here.
			 * Note that the "approximate" parameter was not handled by the Oracle JDBC
			 * driver before release 10.2.0.4, which may result in an exception here.
			 * @see http://forums.oracle.com/forums/thread.jspa?threadID=210782
			 * @see http://www.oracle.com/technology/software/tech/java/sqlj_jdbc/htdocs/readme_jdbc_10204.html
			 */
			boolean approximate = this.db.dbTypeIs(ConnectedDB.Oracle);
			ResultSet rs = this.schema.getIndexInfo(
					null, schemaName(tableName), tableName(tableName), true, approximate);
			while (rs.next()) {
				String indexKey = rs.getString("INDEX_NAME");
				if (indexKey != null) { // is null when type = tableIndexStatistic, ignore
					if (!result.containsKey(indexKey))
						result.put(indexKey, new ArrayList<String>());
					result.get(indexKey).add(rs.getString("COLUMN_NAME"));
				}
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception (unable to determine unique columns)", ex);
		}
	}	
	
	/**
	 * Returns a list of imported or exported (foreign) keys for a table.
	 * @param tableName The table we are interested in
	 * @param direction If set to {@link #KEYS_IMPORTED}, the table's foreign keys are returned.
	 * 					If set to {@link #KEYS_EXPORTED}, the table's primary keys referenced from other tables are returned.
	 * @return A list of {@link Join}s; the local columns are in attributes1() 
	 */
	public List<Join> foreignKeys(RelationName tableName, int direction) {
		try {
			Map<String,ForeignKey> fks = new HashMap<String,ForeignKey>();
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
				fks.get(fkName).addColumns(keySeq, foreignColumn, primaryColumn);
			}
			rs.close();
			List<Join> results = new ArrayList<Join>();
			Iterator<ForeignKey> it = fks.values().iterator();
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
	 * tables (not T), and the constraints cover all columns of T,
	 * and there are no foreign keys from other tables pointing to this table
	 */
	public boolean isLinkTable(RelationName tableName) {
		List<Join> foreignKeys = foreignKeys(tableName, KEYS_IMPORTED);
		if (foreignKeys.size() != 2) return false;
		
		List<Join> exportedKeys = foreignKeys(tableName, KEYS_EXPORTED);
		if (!exportedKeys.isEmpty()) return false;
		
		List<Attribute> columns = listColumns(tableName);
		Iterator<Join> it = foreignKeys.iterator();
		while (it.hasNext()) {
			Join fk = it.next();
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
		} else if ((db.dbTypeIs(ConnectedDB.PostgreSQL) || db.dbTypeIs(ConnectedDB.HSQLDB)) 
				&& "public".equals(schema.toLowerCase())) {
			// Call the tables in PostgreSQL or HSQLDB default schema "FOO", not "PUBLIC.FOO"
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
		private TreeMap<Integer,Attribute> primaryColumns = 
			new TreeMap<Integer,Attribute>();
		private TreeMap<Integer,Attribute> foreignColumns = 
			new TreeMap<Integer,Attribute>();
		private void addColumns(int keySequence, Attribute foreign, Attribute primary) {
			primaryColumns.put(new Integer(keySequence), primary);
			foreignColumns.put(new Integer(keySequence), foreign);
		}
		private Join toJoin() {
			return new Join(
					new ArrayList<Attribute>(foreignColumns.values()),
					new ArrayList<Attribute>(primaryColumns.values()), 
					Join.DIRECTION_RIGHT);
		}
	}

	/**
	 * Looks up a RelationName with the schema in order to retrieve the correct capitalization
	 * 
	 * @param relationName
	 * @return The correctly captialized RelationName
	 */
	public RelationName getCorrectCapitalization(RelationName relationName) {
		if (!relationName.caseUnspecified() || !db.lowerCaseTableNames())
			return relationName;
		
		Iterator<RelationName> it = listTableNames(null).iterator();
		while (it.hasNext()) {
			RelationName r = it.next();
			if (r.equals(relationName))
				return r;
		}
		return null;
	}
}