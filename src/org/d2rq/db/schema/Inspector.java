package org.d2rq.db.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.d2rq.D2RQException;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.types.DataType;
import org.d2rq.db.vendor.Vendor;


/**
 * Inspects a database to retrieve schema information. 
 * 
 * This should be called through {@link SQLConnection} when possible,
 * because that class caches results.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Inspector {
	private final static Logger log = Logger.getLogger(Inspector.class);

	private final Connection connection;
	private final Vendor vendor;
	private final DatabaseMetaData metadata;
 
	public Inspector(Connection connection, Vendor vendor) {
		this.connection = connection;
		this.vendor = vendor;
		try {
			this.metadata = connection.getMetaData();
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}

	private String name(Identifier identifier) {
		if (identifier == null) return null;
		return identifier.getCanonicalName();
	}
	
	/**
	 * Lists available table names.
	 * 
	 * @param searchInSchema Schema to list tables from; <tt>null</tt> to list tables from all schemas
	 * @return A list of table names
	 */
	public List<TableName> getTableNames(String searchInSchema) {
		List<TableName> result = new ArrayList<TableName>();
		try {
			ResultSet rs = metadata.getTables(
					null, searchInSchema, null, new String[] {"TABLE", "VIEW"});
			while (rs.next()) {
				String catalog = rs.getString("TABLE_CAT");
				String schema = rs.getString("TABLE_SCHEM");
				String table = rs.getString("TABLE_NAME");
				if (!vendor.isIgnoredTable(catalog, schema, table)) {
					result.add(vendor.toQualifiedTableName(catalog, schema, table));
				}
			}
			rs.close();
			return result;
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}

	public List<ColumnDef> describeSelectStatement(String sqlQuery) {
		List<ColumnDef> result = new ArrayList<ColumnDef>();
		try {
			PreparedStatement stmt = connection.prepareStatement(sqlQuery);
			try {
				ResultSetMetaData meta = stmt.getMetaData();
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					String name = meta.getColumnLabel(i);
					int type = meta.getColumnType(i);
					String typeName = meta.getColumnTypeName(i);
					int size = meta.getPrecision(i);
					DataType dataType = vendor.getDataType(type, typeName, size);
					if (dataType == null) {
						log.warn("Unknown datatype '" + 
								(size == 0 ? typeName : (typeName + "(" + size + ")")) + 
								"' (" + type + ")");
					}
					boolean isNullable = meta.isNullable(i) != ResultSetMetaData.columnNoNulls;
					result.add(new ColumnDef(
							Identifier.createDelimited(name), dataType, isNullable));
				}
				return result;
			} finally {
				stmt.close();
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
	
	public TableDef describeTableOrView(TableName table) {
		List<ColumnDef> columns = new ArrayList<ColumnDef>();
		try {
			ResultSet rs = metadata.getColumns(
					name(table.getCatalog()),
					name(table.getSchema()),
					name(table.getTable()),
					null);
			try {
				while (rs.next()) {
					String name = rs.getString("COLUMN_NAME");
					boolean isNullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
					Identifier identifier = Identifier.createDelimited(name);
					DataType dataType;
					int type = rs.getInt("DATA_TYPE");
					String typeName = rs.getString("TYPE_NAME").toUpperCase();
					int size = rs.getInt("COLUMN_SIZE");
					dataType = vendor.getDataType(type, typeName, size);
					if (dataType == null) {
						log.warn("Unknown datatype '" + 
								(size == 0 ? typeName : (typeName + "(" + size + ")")) + 
								"' (" + type + ")");
					}
					columns.add(new ColumnDef(identifier, dataType, isNullable));
				}
				if (columns.isEmpty()) return null;
				return new TableDef(table, columns, 
						getPrimaryKey(table), getUniqueKeys(table), 
						getForeignKeys(table));
			} finally {
				rs.close();
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
	
	private Set<Key> getUniqueKeys(TableName table) {
		Map<String,List<Identifier>> indexColumns = 
			new HashMap<String,List<Identifier>>();
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
			boolean approximate = (vendor == Vendor.Oracle);
			ResultSet rs = metadata.getIndexInfo(
					name(table.getCatalog()),
					name(table.getSchema()),
					name(table.getTable()), true, approximate);
			try {
				while (rs.next()) {
					String indexName = rs.getString("INDEX_NAME");
					if (indexName == null) continue; // when type = tableIndexStatistic, ignore
					if (!indexColumns.containsKey(indexName)) {
						indexColumns.put(indexName, new ArrayList<Identifier>());
					}
					indexColumns.get(indexName).add(
							Identifier.createDelimited(rs.getString("COLUMN_NAME")));
				}
				Set<Key> result = new HashSet<Key>();
				for (List<Identifier> key: indexColumns.values()) {
					result.add(Key.createFromIdentifiers(key));
				}
				return result;
			} finally {
				rs.close();
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
		
	}

	private Key getPrimaryKey(TableName table) {
		List<Identifier> result = new ArrayList<Identifier>();
		try {
			ResultSet rs = this.metadata.getPrimaryKeys(
					name(table.getCatalog()), 
					name(table.getSchema()), 
					name(table.getTable()));
			try {
				while (rs.next()) {
					result.add(Identifier.createDelimited(rs.getString("COLUMN_NAME")));
				}
				if (result.isEmpty()) return null;
				return Key.createFromIdentifiers(result);
			} finally {
				rs.close();
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
	
	/**
	 * @return <code>true</code> if another table has a foreign key referencing
	 * 		this table's primary key
	 */
	public boolean isReferencedByForeignKey(TableName table) {
		try {
			ResultSet rs = metadata.getExportedKeys(
					name(table.getCatalog()), 
					name(table.getSchema()), 
					name(table.getTable()));
			try {
				return rs.next();
			} finally {
				rs.close();
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
	
	/**
	 * Returns a list of foreign keys for a table.
	 * 
	 * @param tableName The table we are interested in
	 * @return A list of {@link ForeignKey}s; the local columns are in attributes1() 
	 */
	private Set<ForeignKey> getForeignKeys(TableName table) {
		Map<String,ForeignKeyBuilder> fks = new HashMap<String,ForeignKeyBuilder>();
		try {
			ResultSet rs = metadata.getImportedKeys(
					name(table.getCatalog()), 
					name(table.getSchema()), 
					name(table.getTable()));
			try {
				while (rs.next()) {
					TableName pkTable = vendor.toQualifiedTableName(
							rs.getString("PKTABLE_CAT"), 
							rs.getString("PKTABLE_SCHEM"), 
							rs.getString("PKTABLE_NAME"));
					Identifier primaryColumn = Identifier.createDelimited(rs.getString("PKCOLUMN_NAME"));
					Identifier foreignColumn = Identifier.createDelimited(rs.getString("FKCOLUMN_NAME"));
					String fkName = rs.getString("FK_NAME");
					if (!fks.containsKey(fkName)) {
						fks.put(fkName, new ForeignKeyBuilder(pkTable));
					}
					int keySeq = rs.getInt("KEY_SEQ") - 1;
					fks.get(fkName).addColumns(keySeq, foreignColumn, primaryColumn);
				}
				Set<ForeignKey> result = new HashSet<ForeignKey>();
				for (ForeignKeyBuilder fk: fks.values()) {
					fk.toForeignKey();
				}
				return result;
			} finally {
				rs.close();
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}	

	/**
	 * A foreign key. Supports adding (local column, other column) pairs. The pairs
	 * can be added out of order and will be re-ordered internally. When all
	 * columns are added, a {@link ForeignKey} object can be created.
	 */
	private class ForeignKeyBuilder {
		private final TableName referencedTable;
		private final TreeMap<Integer,Identifier> foreignKeyColumns = 
			new TreeMap<Integer,Identifier>();
		private final TreeMap<Integer,Identifier> primaryKeyColumns = 
			new TreeMap<Integer,Identifier>();
		private ForeignKeyBuilder(TableName referencedTable) {
			this.referencedTable = referencedTable;
		}
		private void addColumns(int keySequence, Identifier foreign, Identifier primary) {
			foreignKeyColumns.put(new Integer(keySequence), foreign);
			primaryKeyColumns.put(new Integer(keySequence), primary);
		}
		private ForeignKey toForeignKey() {
			return new ForeignKey(
					Key.createFromIdentifiers(new ArrayList<Identifier>(foreignKeyColumns.values())),
					Key.createFromIdentifiers(new ArrayList<Identifier>(primaryKeyColumns.values())), 
					referencedTable);
		}
	}

	/**
	 * TODO: Move vendor-specific zerofill stuff to vendor.MySQL class
	 * TODO: This should be cached in {@link TableDef} and not called through ConnectedDB
	 */
	public boolean isZerofillColumn(ColumnName column) {
		if (!column.isQualified() || vendor != Vendor.MySQL) return false;
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(
					"DESCRIBE " + vendor.toString(column.getQualifier()));		
			try {
				while (rs.next()) {
					// MySQL names are case insensitive, so we normalize to lower case
					if (column.getColumn().getName().toLowerCase().equals(rs.getString("Field").toLowerCase())) {
						return rs.getString("Type").toLowerCase().indexOf("zerofill") != -1;
					}
				}
				throw new D2RQException("Column not found in DESCRIBE result: " + column,
						D2RQException.SQL_COLUMN_NOT_FOUND);
			} finally {
				if (rs != null) rs.close();
				if (stmt != null) stmt.close();			
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex, D2RQException.D2RQ_SQLEXCEPTION);
		}
	}
}