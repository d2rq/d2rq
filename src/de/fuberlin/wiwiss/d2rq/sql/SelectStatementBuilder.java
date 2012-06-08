package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import d2rq.d2r_query;
import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;


/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * Used within TripleResultSets.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SelectStatementBuilder {
	private static final Log log = LogFactory.getLog(d2r_query.class);
	
	private ConnectedDB database;
	private List<ProjectionSpec> selectSpecs = new ArrayList<ProjectionSpec>(10);
	private List<Expression> conditions = new ArrayList<Expression>();
	private Expression cachedCondition = null;
	private boolean eliminateDuplicates = false;
	private AliasMap aliases = AliasMap.NO_ALIASES;
	private Collection<RelationName> mentionedTables = new HashSet<RelationName>(5); // in their alias forms	
	private List<OrderSpec> orderSpecs;
	private int limit;
	
	public SelectStatementBuilder(Relation relation) {
		if (relation.isTrivial()) {
			throw new IllegalArgumentException("Cannot create SQL for trivial relation");
		}
		if (relation.equals(Relation.EMPTY)) {
			throw new IllegalArgumentException("Cannot create SQL for empty relation");
		}
		database = relation.database();
		this.limit = Relation.combineLimits(relation.limit(), database.limit());
		this.orderSpecs = relation.orderSpecs();
		this.aliases = this.aliases.applyTo(relation.aliases());
		for (Join join: relation.joinConditions()) {
			for (Attribute attribute1: join.attributes1()) {
				Attribute attribute2 = join.equalAttribute(attribute1);
				addCondition(Equality.createAttributeEquality(attribute1, attribute2));
			}
		}
		addCondition(relation.condition());
		addCondition(relation.softCondition());
		for (ProjectionSpec projection: relation.projections()) {
			addSelectSpec(projection);
		}
		eliminateDuplicates = !relation.isUnique();
		addCondition(database.vendor().getRowNumLimitAsExpression(limit));
	
		addMentionedTablesFromConditions();
		
		if (eliminateDuplicates) {
			for (ProjectionSpec projection: selectSpecs) {
				for (Attribute column: projection.requiredAttributes()) {
					if (!database.columnType(aliases.originalOf(column)).supportsDistinct()) {
						log.info("Attempting to apply DISTINCT to relation: " + relation);
						throw new D2RQException("Bug in engine logic: DISTINCT used with " +
								"datatype (" + database.columnType(column) + ") that " +
								"doesn't support it", 
								D2RQException.DATATYPE_DOES_NOT_SUPPORT_DISTINCT);
					}
				}
			}
		}
	}
	
	private Expression condition() {
		if (this.cachedCondition == null) {
			this.cachedCondition = Conjunction.create(this.conditions);
		}
		return this.cachedCondition;
	}
	
	private void addMentionedTablesFromConditions() {
		for (Attribute column: condition().attributes()) {
			this.mentionedTables.add(column.relationName());
		}
	}
	
	public String getSQLStatement() {
		
		StringBuffer result = new StringBuffer("SELECT ");
		
		if (this.eliminateDuplicates) {
			result.append("DISTINCT ");
		}

		String s = database.vendor().getRowNumLimitAsSelectModifier(limit);
		if (!"".equals(s)) {
			result.append(s);
			result.append(" ");
		}
		
		Iterator<ProjectionSpec> it = this.selectSpecs.iterator();
		if (!it.hasNext()) {
			result.append("1");
		}
		while (it.hasNext()) {
			ProjectionSpec projection = it.next();
			result.append(projection.toSQL(database, aliases));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		
		
		result.append(" FROM ");
		Iterator<RelationName> tableIt = mentionedTables.iterator();
		while (tableIt.hasNext()) {			
			RelationName tableName = tableIt.next();
			if (this.aliases.isAlias(tableName)) {
				result.append(database.vendor().getRelationNameAliasExpression(
						aliases.originalOf(tableName), tableName));
			} else {
				result.append(database.vendor().quoteRelationName(tableName));
			}
			if (tableIt.hasNext()) {
				result.append(", ");
			}
		
		}
		
		if (!condition().isTrue()) {
			result.append(" WHERE ");
			result.append(condition().toSQL(this.database, this.aliases));
		}

		Iterator<OrderSpec> orderIt = orderSpecs.iterator();
		if (orderIt.hasNext()) {
			result.append(" ORDER BY ");
		}
		while (orderIt.hasNext()) {
			result.append(orderIt.next().toSQL(database, aliases));
			if (orderIt.hasNext()) {
				result.append(", ");
			}
		}
		
		s = database.vendor().getRowNumLimitAsQueryAppendage(limit);
		if (!"".equals(s)) {
			result.append(" ");
			result.append(s);
		}
						
		return result.toString();
	}
	
	/**
	 * Returns the projection specs used in this query, in order of appearance 
	 * in the "SELECT x, y, z" part of the query.
	 * 
	 * @return A list of {@link ProjectionSpec}s
	 */
	public List<ProjectionSpec> getColumnSpecs() {
		return this.selectSpecs;
	}
	
	/**
	 * Adds a {@link ProjectionSpec} to the SELECT part of the query.
	 * @param projection
	 */
	private void addSelectSpec(ProjectionSpec projection) {
		if (this.selectSpecs.contains(projection)) {
			return;
		}
		for (Attribute attribute: projection.requiredAttributes()) {
			this.mentionedTables.add(attribute.relationName());
		}
		this.selectSpecs.add(projection);
	}

	private void addCondition(Expression condition) {
		this.conditions.add(condition);
		this.cachedCondition = null;
	}
}
