package org.d2rq.db.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TableDef {
	private final TableName name;
	private final List<ColumnDef> columns;
	private final List<Identifier> columnNameList;
	private final IdentifierList primaryKey;
	private final Set<IdentifierList> uniqueKeys;
	private final Set<ForeignKey> foreignKeys;
	
	public TableDef(TableName name,
			List<ColumnDef> columns, 
			IdentifierList primaryKey, 
			Set<IdentifierList> uniqueKeys,
			Set<ForeignKey> foreignKeys) {
		this.name = name;
		this.columns = columns;
		this.columnNameList = new ArrayList<Identifier>(columns.size());
		for (ColumnDef column: columns) {
			columnNameList.add(column.getName());
		}
		this.primaryKey = primaryKey;
		this.uniqueKeys = uniqueKeys;
		this.foreignKeys = foreignKeys;
	}

	public TableName getName() {
		return name;
	}

	public List<ColumnDef> getColumns() {
		return columns;
	}

	public List<Identifier> getColumnNames() {
		return columnNameList;
	}
	
	public ColumnDef getColumnDef(Identifier columnName) {
		for (ColumnDef column: columns) {
			if (column.getName().equals(columnName)) {
				return column;
			}
		}
		return null;
	}
	
	public IdentifierList getPrimaryKey() {
		return primaryKey;
	}

	public Set<IdentifierList> getUniqueKeys() {
		return uniqueKeys;
	}

	public Set<ForeignKey> getForeignKeys() {
		return foreignKeys;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof TableDef)) return false;
		TableDef other = (TableDef) o;
		return name.equals(other.name) && columns.equals(other.columns) && 
				(primaryKey == null ? other.primaryKey == null : primaryKey.equals(other.primaryKey)) && 
				uniqueKeys.equals(other.uniqueKeys) && 
				foreignKeys.equals(other.foreignKeys);
	}
	
	public int hashCode() {
		return name.hashCode() ^ columns.hashCode() ^ 
				(primaryKey == null ? 0 : primaryKey.hashCode()) ^ 
				uniqueKeys.hashCode() ^ foreignKeys.hashCode();
	}
}
