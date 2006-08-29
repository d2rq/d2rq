/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Join;

/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * Used within TripleResultSets.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilder.java,v 1.2 2006/08/29 15:13:12 cyganiak Exp $
 */

public class SelectStatementBuilder {
	private final static Pattern escapePattern = Pattern.compile("([\\\\'])");
	private final static String escapeReplacement = "\\\\$1";
	private Database database;
	private List sqlSelect = new ArrayList(10);
	private List sqlWhere = new ArrayList(15);
	/** Maps column names from the database to columns numbers in the result set. */
	private Map columnNameNumber = new HashMap(10);
	private int selectColumnCount = 0;
	private boolean eliminateDuplicates = false;
	
	// see SQLStatementMaker.sqlFromExpression(referredTables,aliasMap)
	protected AliasMap aliases = AliasMap.NO_ALIASES;
	protected Collection mentionedTables = new HashSet(5); // Strings in their alias forms	

	public SelectStatementBuilder(Database database) {
		this.database = database;
	}
	
	public Database getDatabase() {
	    return this.database;
	}

	public String getSQLStatement() {
		StringBuffer result = new StringBuffer("SELECT ");
		if (this.eliminateDuplicates) {
			result.append("DISTINCT ");
		}
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
		it = mentionedTables.iterator();
		if (it.hasNext()) {
			result.append(" FROM ");
		}
		while (it.hasNext()) {			
			String tableName = (String) it.next();
			if (this.aliases.isAlias(tableName)) {
				result.append(this.aliases.originalOf(tableName));
				result.append(" AS ");
			}
			result.append(tableName);
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
		return result.toString();
	}
	
	public void addAliasMap(AliasMap newAliases) {
		this.aliases = this.aliases.applyTo(newAliases);
	}
	
	private void referTable(String tableName) {
		if (!mentionedTables.contains(tableName)) {
			mentionedTables.add(tableName);
		}
	}

	/**
	 * Adds a column to the SELECT part of the query.
	 * @param column the column
	 */
	public void addSelectColumn(Column column) {
		String qualName=column.getQualifiedName();
		if (this.sqlSelect.contains(qualName)) {
			return;
		}
		this.sqlSelect.add(qualName);
		this.columnNameNumber.put(qualName,
				new Integer(this.selectColumnCount));
		this.mentionedTables.add(column.getTableName());
		this.selectColumnCount++;
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
		String whereClause = column.getQualifiedName() + "=" + correctlyQuotedColumnValue(column,value); 
		if (this.sqlWhere.contains(whereClause)) {
			return;
		}
		this.sqlWhere.add(whereClause);
		this.mentionedTables.add(column.getTableName());
	}
	
	public String correctlyQuotedColumnValue(Column column, String value) {
	    return getQuotedColumnValue(value, columnType(column));
	}
	
	public int columnType(Column column) {
	    Column physicalColumn = this.aliases.originalOf(column);
	    return this.database.getColumnType(physicalColumn.getQualifiedName());
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
	 * TODO This should also add columns to mentionedTables
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
			String expression=join.sqlExpression();
			if (this.sqlWhere.contains(expression)) {
				continue;
			}
			this.sqlWhere.add(expression);
			referTable(join.getFirstTable());
			referTable(join.getSecondTable());
        }
    }

	/**
	 * Make columns accessible through their old, pre-renaming names.
	 * TODO: Use AliasMap instead of HashMap
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

	/**
	 * Sets if the SQL statement should eliminate duplicate rows
	 * ("SELECT DISTINCT").
	 * @param eliminateDuplicates enable DISTINCT?
	 */
	public void setEliminateDuplicates(boolean eliminateDuplicates) {
		this.eliminateDuplicates = eliminateDuplicates;
	}

	public Map getColumnNameNumberMap() {
		return this.columnNameNumber;
	}

	private static String getQuotedColumnValue(String value, int columnType) {
		if (Database.numericColumnType==columnType) {
			// convert to number and back to String to avoid SQL injection
			try {
				return Integer.toString(Integer.parseInt(value));
			} catch (NumberFormatException nfex) {
				try {
					return Double.toString(Double.parseDouble(value));
				} catch (NumberFormatException nfex2) {
					return "NULL";
				}
			}
		} else if (Database.dateColumnType==columnType) {
			return "#" + value + "#";
		}
		return "'" + SelectStatementBuilder.escape(value) + "'";
	}
	
	/**
	 * Escape special characters in database literals to avoid
	 * SQL injection
	 */
	public static String escape(String s) {
		return SelectStatementBuilder.escapePattern.matcher(s).
				replaceAll(SelectStatementBuilder.escapeReplacement);
	}
}
