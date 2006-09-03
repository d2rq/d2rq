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
import de.fuberlin.wiwiss.d2rq.map.Expression;
import de.fuberlin.wiwiss.d2rq.map.Join;

/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * Used within TripleResultSets.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilder.java,v 1.7 2006/09/03 17:59:08 cyganiak Exp $
 */

public class SelectStatementBuilder {
	private final static Pattern singleQuoteEscapePattern = Pattern.compile("([\\\\'])");
	private final static String singleQuoteEscapeReplacement = "\\\\$1";
	private final static Pattern backtickEscapePattern = Pattern.compile("([\\\\`])");
	private final static String backtickEscapeReplacement = "$1$1";
	
	/**
	 * Wraps s in single quotes and escapes special characters to avoid SQL injection
	 */
	public static String singleQuote(String s) {
		return "'" + SelectStatementBuilder.singleQuoteEscapePattern.matcher(s).
				replaceAll(SelectStatementBuilder.singleQuoteEscapeReplacement) + "'";
	}

	/**
	 * Wraps s in backticks and escapes special characters to avoid SQL injection
	 */
	public static String backtickQuote(String s) {
		return "`" + SelectStatementBuilder.backtickEscapePattern.matcher(s).
				replaceAll(SelectStatementBuilder.backtickEscapeReplacement) + "`";
	}

	/**
	 * @param s Any String
	 * @return <tt>true</tt> if it is an SQL reserved word
	 * @see {@link ReservedWords}
	 */
	public static boolean isReservedWord(String s) {
		return ReservedWords.contains(s);
	}
	
	private Database database;
	private List sqlSelect = new ArrayList(10);
	private List conditions = new ArrayList();
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

	public boolean isTrivial() {
		return this.sqlSelect.isEmpty() && this.conditions.isEmpty();
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
				result.append(quoteTableName(this.aliases.originalOf(tableName)));
				result.append(" AS ");
			}
			result.append(quoteTableName(tableName));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		it = this.conditions.iterator();
		if (it.hasNext()) {
			result.append(" WHERE ");
			while (it.hasNext()) {
				Expression condition = (Expression) it.next();
				result.append(condition.toSQL());
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
		mentionedTables.add(tableName);
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
		Expression condition = new Expression(
				column.getQualifiedName() + " = " +
				correctlyQuotedColumnValue(column,value));
		if (this.conditions.contains(condition)) {
			return;
		}
		this.conditions.add(condition);
		referTable(column.getTableName());
	}
	
	public String correctlyQuotedColumnValue(Column column, String value) {
	    return getQuotedColumnValue(value, columnType(column));
	}
	
	public int columnType(Column column) {
	    Column physicalColumn = this.aliases.originalOf(column);
	    return this.database.getColumnType(physicalColumn.getQualifiedName());
	}
	
	private String quoteTableName(String tableName) {
		if (!isReservedWord(tableName)) {
			// No need to quote
			return tableName;
		}
		if ("MySQL".equals(this.database.getType())) {
			return backtickQuote(tableName);
		}
		// Not MySQL -- We just return the plain table name without
		// quoting because I'm not sure how other RDBMSes handle this
		return tableName;
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
	 * Adds a WHERE clause to the query.
	 * @param An SQL expression
	 */
	public void addCondition(Expression condition) {
		if (condition.isTrue() || this.conditions.contains(condition)) {
			return;
		}
		this.conditions.add(condition);
		Iterator it = condition.columns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			referTable(column.getTableName());
		}
	}

	public void addJoins(Set joins) {
		Iterator it = joins.iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			Expression expression = new Expression(join.sqlExpression());
			if (this.conditions.contains(expression)) {
				continue;
			}
			this.conditions.add(expression);
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
			// Check if it actually is a number to avoid SQL injection
			try {
				return Integer.toString(Integer.parseInt(value));
			} catch (NumberFormatException nfex) {
				try {
					return Double.toString(Double.parseDouble(value));
				} catch (NumberFormatException nfex2) {
					// No number -- return as quoted string
					// DBs seem to interpret non-number strings as 0
					return SelectStatementBuilder.singleQuote(value);
				}
			}
		} else if (Database.dateColumnType==columnType) {
			return "#" + value + "#";
		}
		return SelectStatementBuilder.singleQuote(value);
	}
}
