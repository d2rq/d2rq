/*
 * $Id: Join.java,v 1.2 2004/08/09 20:16:52 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents an SQL join between two tables, spanning one or more columns.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
class Join {
	private Set fromColumns = new HashSet(2);
	private Set toColumns = new HashSet(2);
	private Map otherSide = new HashMap(4);
	private String fromTable = null;
	private String toTable = null;

	public void addCondition(String joinCondition) {
		Column col1 = Join.getColumn(joinCondition, true);
		Column col2 = Join.getColumn(joinCondition, false);
		if (this.fromTable == null) {
			this.fromTable = col1.getTableName();
			this.toTable = col2.getTableName();
		}
		this.fromColumns.add(col1);
		this.toColumns.add(col2);
		this.otherSide.put(col1, col2);
		this.otherSide.put(col2, col1);
	}

	public boolean containsTable(String tableName) {
		return tableName.equals(this.fromTable) ||
				tableName.equals(this.toTable);
	}

	public boolean containsColumn(Column column) {
		return this.fromColumns.contains(column) ||
				this.toColumns.contains(column);
	}

	public String getFirstTable() {
		return this.fromTable;
	}
	
	public String getSecondTable() {
		return this.toTable;
	}

	public Set getFirstColumns() {
		return this.fromColumns;
	}

	public Set getSecondColumns() {
		return this.toColumns;
	}

	/**
	 * Checks if one side of the join is equal to the argument set of
	 * {@link Column}s.
	 * @param columns a set of Column instances
	 * @return <tt>true</tt> if one side of the join is equal to the column set
	 */
	public boolean isOneSide(Set columns) {
		return this.fromColumns.equals(columns) || this.toColumns.equals(columns);
	}

	public Column getOtherSide(Column column) {
		return (Column) this.otherSide.get(column);
	}

	private static Column getColumn(String joinCondition, boolean first) {
		int index = joinCondition.indexOf("=");
		if (index == -1) {
			Logger.instance().error("Illegal d2rq:join: \"" + joinCondition +
					"\" (must be in Table1.Col1=Table2.Col2 form)");
			return null;
		}
		String col1 = joinCondition.substring(0, index).trim();
		String col2 = joinCondition.substring(index + 1).trim();
		boolean return1 = col1.compareTo(col2) < 0;
		if (!first) {
			return1 = !return1;
		}
		return return1 ? new Column(col1) : new Column(col2);
	}
	
	/**
	 * Builds a list of Join objects from a list of join condition
	 * strings. Groups multiple condition that connect the same
	 * two table (multi-column keys) into a single join.
	 * @param joinConditions a collection of strings
	 * @return a set of Join instances
	 */
	public static Set buildJoins(Collection joinConditions) {
		Set result = new HashSet(2);
		Iterator it = joinConditions.iterator();
		while (it.hasNext()) {
			String condition = (String) it.next();
			String table1 = Join.getColumn(condition, true).getTableName();
			String table2 = Join.getColumn(condition, false).getTableName();
			Iterator it2 = result.iterator();
			while (it2.hasNext()) {
				Join join = (Join) it2.next();
				if (join.containsTable(table1) && join.containsTable(table2)) {
					join.addCondition(condition);
					condition = null;
					break;
				}
			}
			if (condition != null) {
				Join join = new Join();
				join.addCondition(condition);
				result.add(join);
			}
		}
		return result;
	}
	
	public String toString() {
		Object[] from = this.fromColumns.toArray();
		Object[] to = this.toColumns.toArray();
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < from.length; i++) {
			if (i > 0) {
				result.append(" AND ");
			}
			result.append(from[i].toString());
			result.append("=");
			result.append(to[i].toString());
		}
		return result.toString();
	}
}
