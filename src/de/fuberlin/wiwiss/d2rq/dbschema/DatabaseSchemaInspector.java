package de.fuberlin.wiwiss.d2rq.dbschema;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

/**
 * Inspects a database to retrieve schema information. 
 * 
 * TODO: All the dbType checks should be moved to the {@link Vendor} subclasses
 * TODO: This usually shouldn't be used directly, but through the ConnectedDB.
 *       Except in the MappingGenerator. ConnectedDB is easier mockable for unit tests! 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class DatabaseSchemaInspector {
	private final static Logger log = Logger.getLogger(DatabaseSchemaInspector.class);

	private final ConnectedDB db;
	private final DatabaseMetaData schema;
 
	public static final int KEYS_IMPORTED = 0;
	public static final int KEYS_EXPORTED = 1;
	
	public DatabaseSchemaInspector(ConnectedDB db) {
		this.db = db;
		try {
			this.schema = db.connection().getMetaData();
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}

	/**
	 * @param column
	 * @return The column's datatype, or <code>null</code> if unknown
	 */
	public DataType columnType(Attribute column) {
		try {
			ResultSet rs = this.schema.getColumns(null, column.schemaName(), 
					column.tableName(), column.attributeName());
			try {
				if (!rs.next()) {
					throw new D2RQException("Column " + column + " not found in database",
							D2RQException.SQL_COLUMN_NOT_FOUND);
				}
				int type = rs.getInt("DATA_TYPE");
				String name = rs.getString("TYPE_NAME").toUpperCase();
				int size = rs.getInt("COLUMN_SIZE");
				DataType result = db.vendor().getDataType(type, name, size);
				if (result == null) {
					log.warn("Unknown datatype '" + (size == 0 ? name : (name + "(" + size + ")")) + "' (" + type + ")");
				}
				return result;
			} finally {
				rs.close();
			}
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
	
	public boolean isNullable(Attribute column) {
		try {
			ResultSet rs = this.schema.getColumns(null, column.schemaName(), 
					column.tableName(), column.attributeName());
			if (!rs.next()) {
				throw new D2RQException("Column " + column + " not found in database",
						D2RQException.SQL_COLUMN_NOT_FOUND);
			}
			boolean nullable = (rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
			rs.close();
			return nullable;
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
	
	public boolean isZerofillColumn(Attribute column) {
		boolean isZerofill = false;
		boolean foundColumn = false;
		
		try {
			if (db.vendor() != Vendor.MySQL) return false;
			Statement stmt = db.connection().createStatement();

			db.vendor().beforeQuery(db.connection());
			ResultSet rs = stmt.executeQuery("DESCRIBE " + db.vendor().quoteRelationName(column.relationName()));		
			db.vendor().afterQuery(db.connection());

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
			throw new D2RQException("Database exception", ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
		throw new D2RQException("Column not found in DESCRIBE result: " + column,
				D2RQException.SQL_COLUMN_NOT_FOUND);
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
				if (!this.db.vendor().isIgnoredTable(schema, table)) {
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
			throw new D2RQException("Database exception", ex, D2RQException.D2RQ_SQLEXCEPTION);
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
			throw new D2RQException("Database exception", ex, D2RQException.D2RQ_SQLEXCEPTION);
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
			boolean approximate = (db.vendor() == Vendor.Oracle);
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
			throw new D2RQException("Database exception (unable to determine unique columns)", 
					ex, D2RQException.D2RQ_SQLEXCEPTION);
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
			ResultSet rs = (direction == KEYS_IMPORTED
					? this.schema.getImportedKeys(null, schemaName(tableName), tableName(tableName))
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
			throw new D2RQException("Database exception", ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}	

	private String schemaName(RelationName tableName) {
		if (this.db.vendor() == Vendor.PostgreSQL && tableName.schemaName() == null) {
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
		} else if ((db.vendor() == Vendor.PostgreSQL || db.vendor() == Vendor.HSQLDB) 
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