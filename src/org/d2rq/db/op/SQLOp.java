package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;


/**
 * A SQL SELECT statement. The query is given as a string. The exported
 * column names are unqualified.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQLOp implements DatabaseOp {
	private final SQLConnection sqlConnection;
	private final String sql;
	private final List<Identifier> columnIDs;
	private final List<ColumnName> columnNames = new ArrayList<ColumnName>();
	private final Map<Identifier,ColumnDef> columnMetadata = 
		new HashMap<Identifier,ColumnDef>();
	
	public SQLOp(SQLConnection sqlConnection, String sql,
			List<ColumnDef> columns) {
		this.sqlConnection = sqlConnection;
		this.sql = sql;
		columnIDs = new ArrayList<Identifier>(columns.size());
		for (ColumnDef column: columns) {
			columnIDs.add(column.getName());
			columnMetadata.put(column.getName(), column);
			columnNames.add(ColumnName.create(column.getName()));
		}
	}
	
	public String getSQL() {
		return sql;
	}
	
	public TableName getTableName() {
		return null;
	}
	
	public boolean hasColumn(ColumnName column) {
		if (column.isQualified()) return false;
		return columnIDs.contains(column.getColumn());
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
		// TODO: Can we do something smarter?
		return Collections.emptySet();
	}
	
	public SQLConnection getSQLConnection() {
		return sqlConnection;
	}
	
	public void accept(OpVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return sql;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SQLOp)) {
			return false;
		}
		return sql.equals(((SQLOp) other).sql);
	}
	
	@Override
	public int hashCode() {
		return sql.hashCode() ^ 3457;
	}
}
