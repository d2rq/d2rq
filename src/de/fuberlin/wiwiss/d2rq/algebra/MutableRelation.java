package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;


/**
 * Wraps a relation and allows it to be modified by relational
 * operators. Normally, applying an operator to a relation
 * results in a new object. This is impractical in some places.
 * The MutableRelation solves this problem.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MutableRelation implements RelationalOperators {
	private Relation relation;
	
	public MutableRelation(Relation initialState) {
		this.relation = initialState;
	}
	
	public Relation immutableSnapshot() {
		return this.relation;
	}
	
	public Relation renameColumns(ColumnRenamer renamer) {
		return this.relation = this.relation.renameColumns(renamer);
	}

	public Relation empty() {
		return this.relation = Relation.EMPTY;
	}
	
	public Relation select(Expression condition) {
		if (condition.isFalse()) {
			return empty();
		}
		return this.relation = this.relation.select(condition);
	}
    
	public Relation orderBy(List<OrderSpec> orderSpecs) {
		return relation = new RelationImpl(
	            relation.database(),
	            relation.aliases(),
	            relation.condition(),
	            relation.softCondition(),
	            relation.joinConditions(),
	            relation.projections(),
	            relation.isUnique(),
	            orderSpecs,
	            relation.limit(),
	            relation.limitInverse());
	}
	
	public Relation swapLimits() {
	    return relation = new RelationImpl(
	            relation.database(),
	            relation.aliases(),
	            relation.condition(),
	            relation.softCondition(),
	            relation.joinConditions(),
	            relation.projections(),
	            relation.isUnique(),
	            relation.orderSpecs(),
	            relation.limitInverse(),
	            relation.limit());
	}
	
	public Relation project(Set<? extends ProjectionSpec> projectionSpecs) {
		return relation = relation.project(projectionSpecs);
	}
	
	public Relation limit(int limit) {
		return relation = new RelationImpl(
				relation.database(),
	            relation.aliases(),
	            relation.condition(),
	            relation.softCondition(),
	            relation.joinConditions(),
	            relation.projections(),
	            relation.isUnique(),
	            relation.orderSpecs(),
	            Relation.combineLimits(relation.limit(), limit),
				relation.limitInverse());
	}
}
