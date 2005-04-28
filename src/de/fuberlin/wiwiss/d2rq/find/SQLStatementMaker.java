/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq.find;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.map.Alias;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Join;

/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * Used within TripleResultSets.
 *
 * <p>History:<br>
 * 06-07-2004: Initial version of this class.<br>
 * 08-03-2004: Higher-level operations added (addColumnValues etc.)
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */

public class SQLStatementMaker {
	private final static Pattern escapePattern = Pattern.compile("([\\\\'])");
	private final static String escapeReplacement = "\\\\$1";
	private Database database;
	private List sqlSelect = new ArrayList(10);
//	private List sqlFrom = new ArrayList(5); // -> referedTables, aliasMap
	private List sqlWhere = new ArrayList(15);
	/** Maps column names from the database to columns numbers in the result set. */
	private Map columnNameNumber = new HashMap(10);
	private int selectColumnCount = 0;
	private boolean eliminateDuplicates = false;
	
	// see SQLStatementMaker.sqlFromExpression(referredTables,aliasMap)
	protected Map aliasMap=new HashMap(1); // from String (aliased Table) to Alias
	protected Collection referedTables = new HashSet(5); // Strings in their alias forms	

	public SQLStatementMaker(Database database) {
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
		result.append(" FROM ");
		result.append(sqlFromExpression(referedTables,aliasMap));
//		it = this.sqlFrom.iterator();
//		while (it.hasNext()) {
//			result.append(it.next());
//			if (it.hasNext()) {
//				result.append(", ");
//			}
//		}
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
	
	public void addAliasMap(Map m) {
		aliasMap.putAll(m);
	}
	
	private void referTable(String tableName) {
		if (!referedTables.contains(tableName)) {
			referedTables.add(tableName);
		}
	}
	private void referColumn(Column c) {
		String tableName=c.getTableName();
		referTable(tableName);
	}

	private void referColumns(Collection columns) {
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			referColumn((Column) it.next());
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
		this.selectColumnCount++;
		this.columnNameNumber.put(qualName,
				new Integer(this.selectColumnCount));		
		referColumn(column); // jg
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
		referColumn(column);
	}
	
	public String correctlyQuotedColumnValue(Column column, String value) {
	    return getQuotedColumnValue(value, columnType(column));
	}
	
	public int columnType(Column column) {
	    String databaseColumn=column.getQualifiedName(aliasMap);
	    return this.database.getColumnType(databaseColumn);
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
		return "'" + SQLStatementMaker.escape(value) + "'";
	}
	
	/**
	 * Escape special characters in database literals to avoid
	 * SQL injection
	 */
	public static String escape(String s) {
		return SQLStatementMaker.escapePattern.matcher(s).
				replaceAll(SQLStatementMaker.escapeReplacement);
	}
	
	// jg
	private static String sqlFromExpression(Collection referedTables, Map aliasMap) {
		StringBuffer result = new StringBuffer();
		Iterator it=referedTables.iterator();
		int i=0;
		while (it.hasNext()) {			
			if (i > 0) {
				result.append(" , ");
			}
			String tableName=(String)it.next();
			String expression=tableName;
			if (aliasMap!=null) {
				Alias mapVal=(Alias)aliasMap.get(tableName);
				
				if (mapVal!=null) {
					expression=mapVal.sqlExpression();
				} 
			}
			result.append(expression);
			i++;
		}
		return result.toString();
	}

	
}
