package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Expression;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;

/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * Used within TripleResultSets.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilder.java,v 1.15 2006/09/13 14:06:23 cyganiak Exp $
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
	 * @see ReservedWords
	 */
	public static boolean isReservedWord(String s) {
		return ReservedWords.contains(s);
	}
	
	private ConnectedDB database;
	private List selectColumns = new ArrayList(10);
	private List conditions = new ArrayList();
	private boolean eliminateDuplicates = false;
	private AliasMap aliases = AliasMap.NO_ALIASES;
	private Collection mentionedTables = new HashSet(5); // Strings in their alias forms	

	/**
	 * TODO: Try if we can change parameters to (Relation, projectionColumns) and make immutable
	 */
	public SelectStatementBuilder(ConnectedDB database) {
		this.database = database;
	}
	
	public void addRelation(Relation relation) {
		addAliasMap(relation.aliases());
		addJoins(relation.joinConditions());
		addColumnValues(relation.attributeConditions());
		addCondition(relation.condition());
	}
	
	public ConnectedDB getDatabase() {
	    return this.database;
	}

	public boolean isTrivial() {
		return this.selectColumns.isEmpty() && this.conditions.isEmpty();
	}
	
	public String getSQLStatement() {
		StringBuffer result = new StringBuffer("SELECT ");
		if (this.eliminateDuplicates) {
			result.append("DISTINCT ");
		}
		Iterator it = this.selectColumns.iterator();
		if (!it.hasNext()) {
			result.append("1");
		}
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			result.append(column.qualifiedName());
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
	
	/**
	 * Adds a column to the SELECT part of the query.
	 * @param column the column
	 */
	public void addSelectColumn(Attribute column) {
		if (this.selectColumns.contains(column)) {
			return;
		}
		this.mentionedTables.add(column.tableName());
		this.selectColumns.add(column);
	}

    /**
     * Adds a list of {@link Attribute}s to the SELECT part of the query
     * @param columns
     */
	public void addSelectColumns(Set columns) {
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			addSelectColumn((Attribute) it.next());
		}
	}

    /**
     * Adds a WHERE clause to the query. Only records are selected
     * where the column given as the first argument has the value
     * given as second argument.
     * @param column the column whose values are to be restricted
     * @param value the value the column must have
     */
	public void addColumnValue(Attribute column, String value) {
		Expression condition = new Expression(
				column.qualifiedName() + " = " +
				correctlyQuotedColumnValue(column,value));
		if (this.conditions.contains(condition)) {
			return;
		}
		this.conditions.add(condition);
		mentionedTables.add(column.tableName());
	}
	
	private String correctlyQuotedColumnValue(Attribute column, String value) {
	    return getQuotedColumnValue(value, columnType(column));
	}
	
	public int columnType(Attribute column) {
	    return this.database.columnType(this.aliases.originalOf(column));
	}
	
	private String quoteTableName(String tableName) {
		if (!isReservedWord(tableName)) {
			// No need to quote
			return tableName;
		}
		if (this.database.dbTypeIs(ConnectedDB.MySQL)) {
			return backtickQuote(tableName);
		}
		// Not MySQL -- We just return the plain table name without
		// quoting because I'm not sure how other RDBMSes handle this
		return tableName;
	}
	
	/**
	 * Adds multiple WHERE clauses from a map. The map keys are
	 * {@link Attribute} instances. The map values are the values
	 * for those columns.
	 * @param columnsAndValues a map containing columns and their values
	 */
	public void addColumnValues(Map columnsAndValues) {
		Iterator it = columnsAndValues.keySet().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			String value = (String) columnsAndValues.get(column);
			addColumnValue(column, value);
		}	
	}
	
	/**
	 * Adds a WHERE clause to the query.
	 * @param condition An SQL expression
	 */
	public void addCondition(Expression condition) {
		if (condition.isTrue() || this.conditions.contains(condition)) {
			return;
		}
		this.conditions.add(condition);
		Iterator it = condition.columns().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			mentionedTables.add(column.tableName());
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
			mentionedTables.add(join.getFirstTable());
			mentionedTables.add(join.getSecondTable());
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

	private static String getQuotedColumnValue(String value, int columnType) {
		if (columnType == ConnectedDB.NUMERIC_COLUMN) {
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
		} else if (columnType == ConnectedDB.DATE_COLUMN) {
			return "#" + value + "#";
		}
		return SelectStatementBuilder.singleQuote(value);
	}
	
	/**
	 * @return An iterator over {@link ResultRow}s
	 */
	public ClosableIterator execute() {
		if (isTrivial()) {
			return new SingletonIterator(new String[]{});
		}
		return new QueryExecutionIterator(getSQLStatement(), this.selectColumns, this.database);		
	}
}
