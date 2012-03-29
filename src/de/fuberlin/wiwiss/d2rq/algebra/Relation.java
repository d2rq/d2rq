package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * TODO Describe this type
 * TODO Add uniqueConstraints()
 * TODO Explicitly list tables!!!
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class Relation implements RelationalOperators {

	public final static int NO_LIMIT = -1;
	
	public static Relation createSimpleRelation(
			ConnectedDB database, Attribute[] attributes) {
		return new RelationImpl(database, AliasMap.NO_ALIASES, Expression.TRUE, Expression.TRUE,
				Collections.<Join>emptySet(), 
				new HashSet<ProjectionSpec>(Arrays.asList(attributes)), 
				false, Collections.<OrderSpec>emptyList(), -1, -1);
	}
	
	public static Relation EMPTY = new Relation() {
		public ConnectedDB database() { return null; }
		public AliasMap aliases() { return AliasMap.NO_ALIASES; }
		public Set<Join> joinConditions() { return Collections.<Join>emptySet(); }
		public Expression condition() { return Expression.FALSE; }
		public Expression softCondition() { return Expression.FALSE; }
		public Set<ProjectionSpec> projections() { return Collections.<ProjectionSpec>emptySet(); }
		public Relation select(Expression condition) { return this; }
		public Relation renameColumns(ColumnRenamer renamer) { return this; }
		public Relation project(Set<? extends ProjectionSpec>  projectionSpecs) { return this; }
		public boolean isUnique() { return true; }
		public String toString() { return "Relation.EMPTY"; }
		public List<OrderSpec> orderSpecs() {return Collections.emptyList();}
		public int limit() { return Relation.NO_LIMIT; }
		public int limitInverse() { return Relation.NO_LIMIT; }
	};
	public static Relation TRUE = new Relation() {
		public ConnectedDB database() { return null; }
		public AliasMap aliases() { return AliasMap.NO_ALIASES; }
		public Set<Join> joinConditions() { return Collections.<Join>emptySet(); }
		public Expression condition() { return Expression.TRUE; }
		public Expression softCondition() { return Expression.TRUE; }
		public Set<ProjectionSpec> projections() { return Collections.<ProjectionSpec>emptySet(); }
		public Relation select(Expression condition) {
			if (condition.isFalse()) return Relation.EMPTY;
			if (condition.isTrue()) return Relation.TRUE;
			// FIXME This is broken; we need to evaluate the expression, but we don't
			// have a ConnectedDB available here so we can't really do it
			return Relation.TRUE;
		}
		public Relation renameColumns(ColumnRenamer renamer) { return this; }
		public Relation project(Set<? extends ProjectionSpec> projectionSpecs) { return this; }
		public boolean isUnique() { return true; }
		public String toString() { return "Relation.TRUE"; }
		public List<OrderSpec> orderSpecs() {return Collections.emptyList();}
		public int limit() { return Relation.NO_LIMIT; }
		public int limitInverse() { return Relation.NO_LIMIT; }
	};

	// TODO Can we remove this, and maybe pass the database around in an ARQ Context object?
	public abstract ConnectedDB database();
	
	/**
	 * The tables that are used to set up this relation, both in
	 * their aliased form, and with their original physical names.
	 * @return All table aliases required by this relation
	 */
	public abstract AliasMap aliases();
	
	/**
	 * Returns the join conditions that must hold between the tables
	 * in the relation.
	 * @return A set of {@link Join}s 
	 */
	public abstract Set<Join> joinConditions();

	/**
	 * An expression that must be satisfied for all tuples in the
	 * relation.
	 * @return An expression; {@link Expression#TRUE} indicates no condition
	 */
	public abstract Expression condition();
	
	/**
	 * An expression satisfied by all tuples in the relation. This
	 * is a necessary but not sufficient condition; it is
	 * descriptive and not prescriptive. Replacing the condition
	 * with {@link Expression#TRUE} does not change the contents of
	 * the relation. It is thus just an optional condition that can
	 * be used for optimization, but can be dropped or ignored if
	 * convenient. Typically, there is other Java code that ensures the
	 * condition regardless of whether it is present here or not.
	 * 
	 * We use this in particular for adding IS NOT NULL constraints
	 * on all nullable columns that need to have a non-NULL value to form a
	 * triple or binding.
	 * 
	 * @return An expression; {@link Expression#TRUE} indicates no soft condition
	 */
	public abstract Expression softCondition();
	
	/**
	 * The attributes or expressions that the relation is projected to.
	 * @return A set of {@link ProjectionSpec}s
	 */
	public abstract Set<ProjectionSpec> projections();
	
	public abstract boolean isUnique();
	
	/**
	 * The expressions (and ascending/descending flag) used for ordering
	 * the relation.
	 */
	public abstract List<OrderSpec> orderSpecs();

	/**
	 * The limit clause for the SQL result set
	 * @return number of records to return
	 */
	public abstract int limit();

	/**
	 * The limit clause for the SQL result set describing the inverse relation
	 * @return number of records to return
	 */
	public abstract int limitInverse();

	public Set<Attribute> allKnownAttributes() {
		Set<Attribute> results = new HashSet<Attribute>();
		results.addAll(condition().attributes());
		results.addAll(softCondition().attributes());
		for (Join join: joinConditions()) {
			results.addAll(join.attributes1());
			results.addAll(join.attributes2());
		}
		for (ProjectionSpec projection: projections()) {
			results.addAll(projection.requiredAttributes());
		}
		for (OrderSpec order: orderSpecs()) {
			results.addAll(order.expression().attributes());
		}
		return results;
	}

	public Set<RelationName> tables() {
		Set<RelationName> results = new HashSet<RelationName>();
		for (Attribute attribute: allKnownAttributes()) {
			results.add(attribute.relationName());
		}
		return results;
	}
	
	/**
	 * @return <code>true</code> if this is the trivial table (one row, no columns)
	 */
	public boolean isTrivial() {
		return projections().isEmpty() && condition().isTrue() && joinConditions().isEmpty();
	}
		
	public static int combineLimits(int limit1, int limit2) {
	    if(limit1==Relation.NO_LIMIT) {
	        return limit2;
	    } else if(limit2==Relation.NO_LIMIT) {
	        return limit1;
	    }
	    return Math.min(limit1, limit2);
	}
}