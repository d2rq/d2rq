/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.hp.hpl.jena.graph.Node;

/** SQLStatementMakers collect SELECT and WHERE elements and deliver a corresponding SQL statement.
 * They are used within TripleResultSets.
 *
 * <BR>History: 06-07-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */

class SQLStatementMaker {
	private final static Pattern escapePattern = Pattern.compile("([\\\\'])");
	private final static String escapeReplacement = "\\\\$1";
	private Database database;
	private List sqlSelect = new ArrayList(10);
	private List sqlFrom = new ArrayList(5);
	private List sqlWhere = new ArrayList(15);
	/** Maps column names from the database to columns numbers in the result set. */
	private Map columnNameNumber = new HashMap(10);
	private int selectColumnCount = 0;

	public SQLStatementMaker(Database database) {
		this.database = database;
	}

	public String getSQLStatement() {
		StringBuffer result = new StringBuffer("SELECT DISTINCT ");
		Iterator it = this.sqlSelect.iterator();
		if (!it.hasNext()) {
			result.append("1");
		}
		while (it.hasNext()) {
			String columnname = (String) it.next();
			result.append(columnname);
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(" FROM ");
		it = this.sqlFrom.iterator();
		while (it.hasNext()) {
			result.append(it.next());
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		it = this.sqlWhere.iterator();
		if (it.hasNext()) {
			result.append(" WHERE ");
			while (it.hasNext()) {
				result.append(it.next());
				if (it.hasNext()) {
					result.append(" AND ");
				}
			}
		}
		result.append(";");
		return result.toString();
	}

	/**
	 * Adds a column to the SELECT part of the query.
	 * @param column the column
	 */
	public void addSelectColumn(Column column) {
		if (this.sqlSelect.contains(column.getQualifiedName())) {
			return;
		}
		this.sqlSelect.add(column.getQualifiedName());
		this.selectColumnCount++;
		this.columnNameNumber.put(column.getQualifiedName(),
				new Integer(this.selectColumnCount));
		if (this.sqlFrom.contains(column.getTableName())) {
			return;
		}
		this.sqlFrom.add(column.getTableName());
	}

    /**
     * Adds a list of {@link Column}s to the SELECT part of the query
     * @param columns
     */
	public void addSelectColumns(Set columns) {
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			addSelectColumn((Column) it.next());
		}
	}

    /**
     * Adds a WHERE clause to the query. Only records are selected
     * where the column given as the first argument has the value
     * given as second argument.
     * @param column the column whose values are to be restricted
     * @param value the value the column must have
     */
	public void addColumnValue(Column column, String value) {
    		String whereClause = column.getQualifiedName() + "=" +
				getQuotedColumnValue(value, this.database.getColumnType(column));
		if (this.sqlWhere.contains(whereClause)) {
			return;
		}
		this.sqlWhere.add(whereClause);
		if (this.sqlFrom.contains(column.getTableName())) {
			return;
		}
		this.sqlFrom.add(column.getTableName());
	}

	/**
	 * Adds multiple WHERE clauses from a map. The map keys are
	 * {@link Column} instances. The map values are the values
	 * for those columns.
	 * @param columnsAndValues a map containing columns and their values
	 */
	public void addColumnValues(Map columnsAndValues) {
		Iterator it = columnsAndValues.keySet().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			String value = (String) columnsAndValues.get(column);
			addColumnValue(column, value);
		}	
	}

	/**
	 * Adds multiple WHERE clauses to the query.
	 * @param conditions a set of Strings containing SQL WHERE clauses
	 */
	public void addConditions(Set conditions) {
		Iterator it = conditions.iterator();
		while (it.hasNext()) {
			String condition = (String) it.next();
			if (this.sqlWhere.contains(condition)) {
				continue;
			}
			this.sqlWhere.add(condition);			
		}
	}

	public void addJoins(Set joins) {
		Iterator it = joins.iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			if (this.sqlWhere.contains(join.toString())) {
				continue;
			}
			this.sqlWhere.add(join.toString());
			if (!this.sqlFrom.contains(join.getFirstTable())) {
				this.sqlFrom.add(join.getFirstTable());
			}
			if (!this.sqlFrom.contains(join.getSecondTable())) {
				this.sqlFrom.add(join.getSecondTable());
			}
        }
    }

	/**
	 * Make columns accessible through their old, pre-renaming names.
	 * @param renamedColumns
	 */
	public void addColumnRenames(Map renamedColumns) {
		Iterator it = renamedColumns.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			String oldName = ((Column) entry.getKey()).getQualifiedName();
			String newName = ((Column) entry.getValue()).getQualifiedName();
			this.columnNameNumber.put(oldName, this.columnNameNumber.get(newName));
		}
	}

	public Map getColumnNameNumberMap() {
		return this.columnNameNumber;
	}

	private static String getQuotedColumnValue(String value, Node columnType) {
		if (D2RQ.numericColumn.equals(columnType)) {
			return value;
		} else if (D2RQ.dateColumn.equals(columnType)) {
			return "#" + value + "#";
		}
		return "'" + SQLStatementMaker.escape(value) + "'";
	}
	
	/**
	 * Escape special characters in database literals to avoid
	 * SQL injection
	 */
	protected static String escape(String s) {
		return SQLStatementMaker.escapePattern.matcher(s).
				replaceAll(SQLStatementMaker.escapeReplacement);
	}
}
