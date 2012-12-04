package org.d2rq.db.op;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;


/**
 * A {@link DatabaseOp} that can be used directly in the FROM clause of a
 * SQL join. The class doesn't do anything particularly interesting;
 * it is simply used to enforce the rule that only certain kinds of
 * {@link DatabaseOp}s are allowed as children of an InnerJoin.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class NamedOp implements Comparable<NamedOp>, DatabaseOp {
	private final TableName name;
	
	public NamedOp(TableName name) {
		this.name = name;
	}
	
	/**
	 * @return Name in [[CATALOG.]SCHEMA.]TABLE notation
	 */
	public TableName getTableName() {
		return name;
	}
	
	public boolean hasColumn(ColumnName column) {
		if (column.isQualified() && !column.getQualifier().equals(getTableName())) {
			return false;
		}
		for (ColumnName ours: getColumns()) {
			if (ours.getColumn().equals(column.getColumn())) return true;
		}
		return false;
	}

	/**
	 * Relations without schema are less than relations with schema.
	 * Relations without schema are ordered by table name, those with
	 * schema are ordered by schema name.
	 */
	public int compareTo(NamedOp other) {
		return name.compareTo(((NamedOp) other).name);
	}
}
