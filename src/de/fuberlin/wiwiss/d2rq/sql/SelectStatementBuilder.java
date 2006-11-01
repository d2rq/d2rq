package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Expression;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * Used within TripleResultSets.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilder.java,v 1.20 2006/11/01 15:26:58 cyganiak Exp $
 */
public class SelectStatementBuilder {
	private ConnectedDB database;
	private List selectColumns = new ArrayList(10);
	private List conditions = new ArrayList();		// as Strings
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
		if (isTrivial()) {
			return null;
		}
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
			result.append(this.database.quoteAttribute(column));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(" FROM ");
		it = mentionedTables.iterator();
		while (it.hasNext()) {			
			RelationName tableName = (RelationName) it.next();
			if (this.aliases.isAlias(tableName)) {
				result.append(this.database.quoteRelationName(this.aliases.originalOf(tableName)));
				if (this.database.dbTypeIs(ConnectedDB.Oracle)) {
					result.append(" ");
				} else {
					result.append(" AS ");
				}
			}
			result.append(this.database.quoteRelationName(tableName));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		it = this.conditions.iterator();
		if (it.hasNext()) {
			result.append(" WHERE ");
			while (it.hasNext()) {
				result.append((String) it.next());
				if (it.hasNext()) {
					result.append(" AND ");
				}
			}
		}
		if (this.database.limit() != Database.NO_LIMIT) {
			result.append(" LIMIT " + this.database.limit());
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
		this.mentionedTables.add(column.relationName());
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
		String condition = this.database.quoteAttribute(column) + " = " + 
				this.database.quoteValue(value, this.aliases.originalOf(column));
		if (this.conditions.contains(condition)) {
			return;
		}
		this.conditions.add(condition);
		this.mentionedTables.add(column.relationName());
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
		if (condition.isTrue()) {
			return;
		}
		String sql = condition.toSQL(this.database);
		if (this.conditions.contains(sql)) {
			return;
		}
		this.conditions.add(sql);
		Iterator it = condition.columns().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			this.mentionedTables.add(column.relationName());
		}
	}

	public void addJoins(Set joins) {
		Iterator it = joins.iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			String sql = toSQL(join);
			if (this.conditions.contains(sql)) {
				continue;
			}
			this.conditions.add(sql);
			this.mentionedTables.add(join.table1());
			this.mentionedTables.add(join.table2());
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

	private String toSQL(Join join) {
		StringBuffer result = new StringBuffer();
		Iterator it = join.attributes1().iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			result.append(this.database.quoteAttribute(attribute));
			result.append(" = ");
			result.append(this.database.quoteAttribute(join.equalAttribute(attribute)));
			if (it.hasNext()) {
				result.append(" AND ");
			}
		}
		return result.toString();
	}
	
	/**
	 * @return An iterator over {@link ResultRow}s
	 */
	public ClosableIterator execute() {
		if (isTrivial()) {
			return new SingletonIterator(ResultRow.NO_ATTRIBUTES);
		}
		return new QueryExecutionIterator(getSQLStatement(), this.selectColumns, this.database);		
	}
}
