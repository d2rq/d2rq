package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.AttributeEquality;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * Used within TripleResultSets.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilder.java,v 1.22 2006/11/02 21:15:44 cyganiak Exp $
 */
public class SelectStatementBuilder {
	private ConnectedDB database;
	private List selectColumns = new ArrayList(10);
	private List conditions = new ArrayList();		// Expressions
	private Expression cachedCondition = null;
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
		Iterator it = relation.joinConditions().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			addJoin(join);
		}
		addCondition(relation.condition());
	}
	
	public ConnectedDB getDatabase() {
	    return this.database;
	}

	public boolean isTrivial() {
		return this.selectColumns.isEmpty() && condition().isTrue();
	}

	public boolean isEmpty() {
		return condition().isFalse();
	}

	private Expression condition() {
		if (this.cachedCondition == null) {
			this.cachedCondition = Conjunction.create(this.conditions);
		}
		return this.cachedCondition;
	}
	
	private void addMentionedTablesFromConditions() {
		Iterator it = condition().columns().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			this.mentionedTables.add(column.relationName());
		}
	}
	
	public String getSQLStatement() {
		if (isTrivial() || isEmpty()) {
			return null;
		}
		addMentionedTablesFromConditions();
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
		if (!condition().isTrue()) {
			result.append(" WHERE ");
			result.append(condition().toSQL(this.database, this.aliases));
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
	 * Adds a WHERE clause to the query.
	 * @param condition An SQL expression
	 */
	public void addCondition(Expression condition) {
		this.conditions.add(condition);
		this.cachedCondition = null;
	}

	private void addJoin(Join join) {
		Iterator it = join.attributes1().iterator();
		while (it.hasNext()) {
			Attribute attribute1 = (Attribute) it.next();
			Attribute attribute2 = join.equalAttribute(attribute1);
			addCondition(AttributeEquality.create(attribute1, attribute2));
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
	
	/**
	 * @return An iterator over {@link ResultRow}s
	 */
	public ClosableIterator execute() {
		if (isTrivial()) {
			return new SingletonIterator(ResultRow.NO_ATTRIBUTES);
		}
		if (isEmpty()) {
			return NullIterator.instance;
		}
		return new QueryExecutionIterator(getSQLStatement(), this.selectColumns, this.database);		
	}
}
