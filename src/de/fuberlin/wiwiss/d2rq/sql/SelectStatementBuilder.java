package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
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
 * @version $Id: SelectStatementBuilder.java,v 1.32 2009/09/29 19:56:53 cyganiak Exp $
 */
public class SelectStatementBuilder {
	private ConnectedDB database;
	private List selectSpecs = new ArrayList(10);
	private List conditions = new ArrayList();		// Expressions
	private Expression cachedCondition = null;
	private boolean eliminateDuplicates = false;
	private AliasMap aliases = AliasMap.NO_ALIASES;
	private Collection mentionedTables = new HashSet(5); // Strings in their alias forms	
	private Attribute order;
	private boolean orderDesc;
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
		this.order = relation.order();
		this.orderDesc = relation.orderDesc();
		this.aliases = this.aliases.applyTo(relation.aliases());
		Iterator it = relation.joinConditions().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			Iterator it2 = join.attributes1().iterator();
			while (it2.hasNext()) {
				Attribute attribute1 = (Attribute) it2.next();
				Attribute attribute2 = join.equalAttribute(attribute1);
				addCondition(Equality.createAttributeEquality(attribute1, attribute2));
			}
		}
		addCondition(relation.condition());
		it = relation.projections().iterator();
		while (it.hasNext()) {
			addSelectSpec((ProjectionSpec) it.next());
		}
		eliminateDuplicates = !relation.isUnique();
		addCondition(database.getSyntax().getRowNumLimitAsExpression(limit));
	
		addMentionedTablesFromConditions();		
	}
	
	private Expression condition() {
		if (this.cachedCondition == null) {
			this.cachedCondition = Conjunction.create(this.conditions);
		}
		return this.cachedCondition;
	}
	
	private void addMentionedTablesFromConditions() {
		Iterator it = condition().attributes().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			this.mentionedTables.add(column.relationName());
		}
	}
	
	public String getSQLStatement() {
		
		StringBuffer result = new StringBuffer("SELECT ");
		
		if (this.eliminateDuplicates && database.allowDistinct()) {
			result.append("DISTINCT ");
		}

		String s = database.getSyntax().getRowNumLimitAsSelectModifier(limit);
		if (!"".equals(s)) {
			result.append(s);
			result.append(" ");
		}
		
		Iterator it = this.selectSpecs.iterator();
		if (!it.hasNext()) {
			result.append("1");
		}
		while (it.hasNext()) {
			ProjectionSpec projection = (ProjectionSpec) it.next();
			result.append(projection.toSQL(database, aliases));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		
		
		result.append(" FROM ");
		it = mentionedTables.iterator();
		while (it.hasNext()) {			
			RelationName tableName = (RelationName) it.next();
			if (this.aliases.isAlias(tableName)) {
				result.append(database.getSyntax().getRelationNameAliasExpression(
						aliases.originalOf(tableName), tableName));
			} else {
				result.append(database.getSyntax().quoteRelationName(tableName));
			}
			if (it.hasNext()) {
				result.append(", ");
			}
		
		}
		
		if (!condition().isTrue()) {
			result.append(" WHERE ");
			result.append(condition().toSQL(this.database, this.aliases));
		}

		if (order!=null) {
			result.append(" ORDER BY " + order.toSQL(database, aliases) + " ");
			if(orderDesc) {
				result.append("DESC ");
			}
		}
		
		s = database.getSyntax().getRowNumLimitAsQueryAppendage(limit);
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
	public List getColumnSpecs() {
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
		Iterator it = projection.requiredAttributes().iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			this.mentionedTables.add(attribute.relationName());
		}
		this.selectSpecs.add(projection);
	}

	private void addCondition(Expression condition) {
		this.conditions.add(condition);
		this.cachedCondition = null;
	}
}
