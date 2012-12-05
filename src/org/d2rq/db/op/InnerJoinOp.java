package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;



/**
 * An inner join between multiple {@link NamedOp}s.
 * 
 * Note that other kinds of {@link DatabaseOp}s are not allowed as children.
 * This constraint ensures that no additional information
 * (e.g., artificial table names) need to be added during conversion to SQL.
 * When joining of non-{@link NamedOp} is desired, then they need to be
 * wrapped into an {@link AliasOp}, or pulled up above the inner
 * join where possible (e.g., for {@link SelectOp}).
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class InnerJoinOp implements DatabaseOp {
	
	public static DatabaseOp join(NamedOp table1, NamedOp table2, 
			Key key1, Key key2) {
		Collection<NamedOp> tables = new HashSet<NamedOp>();
		tables.add(table1);
		tables.add(table2);
		return InnerJoinOp.join(
				tables, 
				Collections.singleton(
						ColumnListEquality.create(
								table1.getTableName(), key1, 
								table2.getTableName(), key2)));
	}
	
	public static DatabaseOp join(Collection<NamedOp> tables, 
			Set<ColumnListEquality> joinConditions) {
		if (tables.isEmpty()) {
			return DatabaseOp.TRUE;
		}
		if (tables.size() == 1) {
			return tables.iterator().next();
		}
		return new InnerJoinOp(tables, joinConditions);
	}
	
	private final TreeMap<TableName,NamedOp> tablesByName =
		new TreeMap<TableName,NamedOp>();
	private final Map<ColumnName,NamedOp> byColumn =
		new HashMap<ColumnName,NamedOp>();
	private final List<ColumnName> columns = new ArrayList<ColumnName>();
	private final Set<ColumnListEquality> joins;

	private InnerJoinOp(Collection<NamedOp> tables, Set<ColumnListEquality> joins) {
		this.joins = joins;
		for (NamedOp table: tables) {
			columns.addAll(table.getColumns());
			tablesByName.put(table.getTableName(), table);
			for (ColumnName column: table.getColumns()) {
				if (column.isQualified()) {
					byColumn.put(column, table);
				}
				if (byColumn.containsKey(column.getUnqualified())) {
					byColumn.put(column.getUnqualified(), null);
				} else {
					byColumn.put(column.getUnqualified(), table);
				}
			}
		}
	}
	
	public Collection<NamedOp> getTables() {
		return tablesByName.values();
	}
	
	public DatabaseOp getTable(TableName name) {
		return tablesByName.get(name);
	}
	
	public Set<ColumnListEquality> getJoinConditions() {
		return joins;
	}
	
	public TableName getTableName() {
		return null;
	}

	public boolean hasColumn(ColumnName column) {
		return byColumn.containsKey(column) && byColumn.get(column) != null;
	}
	
	public List<ColumnName> getColumns() {
		return columns;
	}

	public boolean isNullable(ColumnName column) {
		return byColumn.containsKey(column) ? byColumn.get(column).isNullable(column) : false;
	}

	public DataType getColumnType(ColumnName column) {
		return byColumn.containsKey(column) ? byColumn.get(column).getColumnType(column) : null;
	}

	public Collection<Key> getUniqueKeys() {
		if (tablesByName.size() == 1) {
			return tablesByName.values().iterator().next().getUniqueKeys();
		}
		// FIXME: If all joins have a unique key over their local side, then we can keep the unique keys
		return Collections.emptySet();
	}

	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			for (DatabaseOp table: tablesByName.values()) {
				table.accept(visitor);
			}
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "InnerJoin(" + tablesByName.values() + (joins.isEmpty() ? ")" : ("," + joins + ")"));
	}
	
	@Override
	public int hashCode() {
		return tablesByName.hashCode() ^ joins.hashCode() ^ 85234;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof InnerJoinOp)) return false;
		InnerJoinOp other = (InnerJoinOp) o;
		return getTables().equals(other.getTables()) && 
				getJoinConditions().equals(other.getJoinConditions());
	}
}
