package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.types.DataType;


public class TableOp extends NamedOp {
	private final TableDef def;
	private final SQLConnection sqlConnection;
	private final List<Identifier> columnIDs;
	private final List<ColumnName> columnNames;
	private final Map<Identifier,ColumnDef> columnMetadata =
		new HashMap<Identifier,ColumnDef>();

	public TableOp(SQLConnection sqlConnection, TableDef tableDefinition) {
		super(tableDefinition.getName());
		this.sqlConnection = sqlConnection;
		this.def = tableDefinition;
		columnIDs = new ArrayList<Identifier>(tableDefinition.getColumns().size());
		columnNames = new ArrayList<ColumnName>(tableDefinition.getColumns().size());
		for (ColumnDef column: tableDefinition.getColumns()) {
			columnIDs.add(column.getName());
			columnMetadata.put(column.getName(), column);
			columnNames.add(ColumnName.create(getTableName(), column.getName()));
		}
	}

	public TableDef getTableDefinition() {
		return def;
	}
	
	public List<ColumnName> getColumns() {
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
	
	public Collection<Key> getUniqueKeys() {
		return def.getUniqueKeys();
	}

	public SQLConnection getSQLConnection() {
		return sqlConnection;
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
