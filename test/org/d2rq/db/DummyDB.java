package org.d2rq.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.d2rq.db.op.TableOp;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.lang.Database;


public class DummyDB extends SQLConnection {
	
	public static DummyTable createTable(String qualifiedName) {
		return new DummyDB().table(qualifiedName);
	}
	
	public static DummyTable createTable(String qualifiedName, String... columns) {
		return new DummyDB().table(qualifiedName, columns);
	}

	private Vendor vendor;
	private int limit = Database.NO_LIMIT;
	private final Map<TableName,DummyTable> dummyTables = new HashMap<TableName,DummyTable>();
	private final Map<TableName,List<ColumnName>> columns = new HashMap<TableName,List<ColumnName>>();
	private final Map<TableName,Collection<Key>> uniqueKeys = new HashMap<TableName,Collection<Key>>();
	private final Map<ColumnName,Boolean> isNullable = new HashMap<ColumnName,Boolean>();
	private final Map<ColumnName,DataType> dataTypes = new HashMap<ColumnName,DataType>();
	
	public DummyDB() {
		this(Vendor.SQL92);
	}
	
	public DummyDB(final Vendor vendor) {
		super(null, null, null, null);
		this.vendor = vendor;
	}

	public void setVendor(Vendor vendor) {
		this.vendor = vendor;
	}

	public void setLimit(int newLimit) {
		limit = newLimit;
	}
	
	public DummyTable table(String qualifiedName) {
		return getTable(TableName.parse(qualifiedName));
	}

	public DummyTable table(String qualifiedName, String... columns) {
		DummyTable result = getTable(TableName.parse(qualifiedName));
		this.columns.put(result.getTableName(), toColumnNames(result.getTableName(), columns));
		return result;
	}

	@Override
	public Vendor vendor() {
		return vendor;
	}
	
	@Override
	public int limit() {
		return limit;
	}
	
	public boolean equals(Object other) {
		return other instanceof DummyDB;
	}

	@Override
	public DummyTable getTable(TableName table) {
		if (!dummyTables.containsKey(table)) {
			dummyTables.put(table, new DummyTable(table));
		}
		return dummyTables.get(table);
	}
	
	private List<ColumnName> toColumnNames(TableName table, String[] columns) {
		List<ColumnName> columnNames = new ArrayList<ColumnName>();
		for (String column: columns) {
			columnNames.add(ColumnName.create(table, Identifier.createUndelimited(column)));
		}
		return columnNames;
	}
	
	public class DummyTable extends TableOp {
		DummyTable(TableName name) {
			super(new TableDef(name, Collections.<ColumnDef>emptyList(),
					null,
					Collections.<Key>emptySet(), 
					Collections.<ForeignKey>emptySet()));
		}
		public void setUniqueKey(String... columns) {
			uniqueKeys.put(getTableName(), Collections.singleton(
					Key.createFromColumns(toColumnNames(getTableName(), columns))));
		}
		private ColumnName toColumnName(String column) {
			return ColumnName.create(getTableName(), Identifier.createDelimited(column));
		}
		public void setIsNullable(String column, boolean flag) {
			isNullable.put(toColumnName(column), flag);
		}
		public void setDataType(String column, DataType dataType) {
			dataTypes.put(toColumnName(column), dataType);
		}
		public void setDataType(String column, GenericType genericType) {
			dataTypes.put(toColumnName(column), genericType.dataTypeFor(vendor()));
		}
		@Override
		public List<ColumnName> getColumns() {
			return columns.get(getTableName()) == null ? Collections.<ColumnName>emptyList() : columns.get(getTableName());
		}
		@Override
		public boolean hasColumn(ColumnName column)  {
			return true;
		}
		@Override
		public boolean isNullable(ColumnName column) {
			column = getTableName().qualifyColumn(column);
			return isNullable.get(getTableName().qualifyColumn(column)) == null ? true : isNullable.get(column);
		}
		@Override
		public DataType getColumnType(ColumnName column) {
			column = getTableName().qualifyColumn(column);
			return dataTypes.get(column) == null ? GenericType.CHARACTER.dataTypeFor(vendor()) : dataTypes.get(column);
		}
		@Override
		public Collection<Key> getUniqueKeys() {
			return uniqueKeys.get(getTableName()) == null ? Collections.<Key>emptySet() : uniqueKeys.get(getTableName());
		}
	}
}
