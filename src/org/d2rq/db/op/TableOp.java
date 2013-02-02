package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.IdentifierList;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.types.DataType;


public class TableOp extends NamedOp {
	private final TableDef def;
	private final List<Identifier> columnIDs;
	private final ColumnList columnNames;
	private final Map<Identifier,ColumnDef> columnMetadata =
		new HashMap<Identifier,ColumnDef>();
	private final Collection<ColumnList> uniqueKeys = new ArrayList<ColumnList>();
	
	public TableOp(TableDef tableDefinition) {
		super(tableDefinition.getName());
		this.def = tableDefinition;
		columnIDs = new ArrayList<Identifier>(tableDefinition.getColumns().size());
		ArrayList<ColumnName> columns = new ArrayList<ColumnName>(tableDefinition.getColumns().size());
		for (ColumnDef columnDef: tableDefinition.getColumns()) {
			columnIDs.add(columnDef.getName());
			columnMetadata.put(columnDef.getName(), columnDef);
			columns.add(getTableName().qualifyIdentifier(columnDef.getName()));
		}
		columnNames = ColumnList.create(columns);
		for (IdentifierList key: def.getUniqueKeys()) {
			uniqueKeys.add(ColumnList.create(getTableName(), key));
		}
	}

	public TableDef getTableDefinition() {
		return def;
	}
	
	public ColumnList getColumns() {
		return columnNames;
	}
	
	public boolean isNullable(ColumnName column) {
		if (!hasColumn(column)) return false; // can't return null...
		return columnMetadata.get(column.getColumn()).isNullable();
	}
	
	public DataType getColumnType(ColumnName column) {
		if (!hasColumn(column)) return null;
		return columnMetadata.get(column.getColumn()).getDataType();
	}
	
	public Collection<ColumnList> getUniqueKeys() {
		return uniqueKeys;
	}

	public void accept(OpVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public String toString() {
		return "Table(" + getTableName() + ")";
	}
	
	@Override
	public int hashCode() {
		return def.hashCode() ^ 998;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TableOp)) return false;
		TableOp other = (TableOp) o;
		return def.equals(other.def);
	}
}
