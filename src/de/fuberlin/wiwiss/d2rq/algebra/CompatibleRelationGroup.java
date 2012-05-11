package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.expr.Disjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;

/**
 * A group of {@link Relation}s that can be combined into a single
 * relation without changing the semantics of {@link de.fuberlin.wiwiss.d2rq.nodes.NodeMaker}s.
 * 
 * Relations can be combined if they access the same database and 
 * they contain exactly the same joins. If they both contain no joins,
 * they must contain only columns from the same table.
 * 
 * Relations that just differ in their <code>WHERE</code> clause (condition)
 * can still be combined, but require that the new relation has the disjunction
 * (<code>OR</code>) of all conditions as a <code>WHERE</code> clause, and
 * the individual conditions must be added to the <code>SELECT</code> list
 * (as {@link ProjectionSpec}s) so that bindings are generated only
 * if that clause is <code>TRUE</code>
 * 
 * TODO: Should check if the BindingMaker already has a condition?
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class CompatibleRelationGroup {

	public static Collection<CompatibleRelationGroup> groupNodeRelations(
			Collection<? extends NodeRelation> nodeRelations) {
		Collection<CompatibleRelationGroup> result = new ArrayList<CompatibleRelationGroup>();
		for (NodeRelation nodeRelation: nodeRelations) {
			addNodeRelation(nodeRelation, result);
		}
		return result;
	}
	
	private static void addNodeRelation(NodeRelation nodeRelation, 
			Collection<CompatibleRelationGroup> groups) {
		for (CompatibleRelationGroup group: groups) {
			if (group.isCompatible(nodeRelation.baseRelation())) {
				group.addBindingMaker(
						nodeRelation.baseRelation(), BindingMaker.createFor(nodeRelation));
				return;
			}
		}
		CompatibleRelationGroup newGroup = new CompatibleRelationGroup();
		newGroup.addBindingMaker(
				nodeRelation.baseRelation(), BindingMaker.createFor(nodeRelation));
		groups.add(newGroup);
	}
	
	private final List<BiningMakerAndCondition> makers = new ArrayList<BiningMakerAndCondition>();
	private Relation firstBaseRelation = null;
	private boolean differentConditions = false;
	private boolean differentSoftConditions = false;
	private boolean allUnique = true;
	private int relationCounter = 0;
	private Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
	private List<OrderSpec> longestOrderSpecs = new ArrayList<OrderSpec>();
	
	public boolean isCompatible(Relation otherRelation) {
		if (firstBaseRelation == null) {
			throw new IllegalStateException();
		}
		if (firstBaseRelation.database()==null || !firstBaseRelation.database().equals(otherRelation.database())) {
			return false;
		}
		if (!firstBaseRelation.joinConditions().equals(otherRelation.joinConditions())) {
			return false;
		}
		Set<RelationName> firstTables = firstBaseRelation.tables();
		Set<RelationName> secondTables = otherRelation.tables();
		if (!firstTables.equals(secondTables)) {
			return false;
		}
		for (RelationName tableName: firstTables) {
			if (!firstBaseRelation.aliases().originalOf(tableName).equals(
					otherRelation.aliases().originalOf(tableName))) {
				return false;
			}
		}
		if (!firstBaseRelation.projections().equals(otherRelation.projections())) {
			// Uniqueness doesn't matter if we project the same columns
			if (!firstBaseRelation.isUnique() || !otherRelation.isUnique()) {
				return false;
			}
		}
		// Compatible ordering?
		for (int i = 0; i < Math.min(longestOrderSpecs.size(), otherRelation.orderSpecs().size()); i++) {
			if (!longestOrderSpecs.get(i).equals(otherRelation.orderSpecs().get(i))) return false;
		}
		for (int i = longestOrderSpecs.size(); i < otherRelation.orderSpecs().size(); i++) {
			longestOrderSpecs.add(otherRelation.orderSpecs().get(i));
		}
		return true;
	}

	public void addRelation(Relation relation) {
		if (firstBaseRelation == null) {
			firstBaseRelation = relation;
			longestOrderSpecs.addAll(firstBaseRelation.orderSpecs());
		}
		if (!relation.condition().equals(firstBaseRelation.condition())) {
			differentConditions = true;
		}
		if (!relation.softCondition().equals(firstBaseRelation.softCondition())) {
			differentSoftConditions = true;
		}
		projections.addAll(relation.projections());
		allUnique = allUnique && relation.isUnique();
		relationCounter++;
	}

	public void addBindingMaker(Relation relation, BindingMaker bindingMaker) {
		addRelation(relation);
		makers.add(new BiningMakerAndCondition(bindingMaker, 
				relation.condition(), relation.softCondition()));
	}
	
	public Relation baseRelation() {
		if (relationCounter == 1) {
			// Just one relation, return it unchanged
			return firstBaseRelation;
		}
		if (differentConditions) {
			// Multiple relations and different conditions, add the conditions
			// as boolean clauses to the SELECT list, and add a new condition
			// consisting of the disjunction (OR) of all conditions
			Set<Expression> allConditions = new HashSet<Expression>();
			Set<ProjectionSpec> projectionsAndConditions = new HashSet<ProjectionSpec>(projections);
			for (BiningMakerAndCondition maker: makers) {
				// If relations A and B have different soft conditions, we want to end up
				// with: (A.cond && A.soft) || (B.cond && B.soft)
				// This is more restrictive than the simpler alternative
				// (A.cond || B.cond) && (A.soft || B.soft), but requires turning
				// the soft conditions into hard conditions.
				allConditions.add(maker.conditionWithSoft());
				if (!maker.condition.isTrue()) {
					projectionsAndConditions.add(maker.conditionProjection());
				}
			}
			if (allConditions.isEmpty()) {
				allConditions.add(Expression.TRUE);
			}
			Disjunction.create(allConditions);
			return new RelationImpl(firstBaseRelation.database(),
					firstBaseRelation.aliases(),
					Disjunction.create(allConditions),
					Expression.TRUE,
					firstBaseRelation.joinConditions(), 
					projectionsAndConditions, 
					allUnique, longestOrderSpecs, firstBaseRelation.limit(), firstBaseRelation.limitInverse());
		} else {
			// Multiple relations with same condition
			Expression softCondition = firstBaseRelation.softCondition();
			if (differentSoftConditions) {
				Set<Expression> allSoftConditions = new HashSet<Expression>();
				for (BiningMakerAndCondition maker: makers) {
					allSoftConditions.add(maker.softCondition);
				}
				if (allSoftConditions.isEmpty()) {
					allSoftConditions.add(Expression.TRUE);
				}
				softCondition = Disjunction.create(allSoftConditions);
			}
			// return a new relation with all the projections
			return new RelationImpl(firstBaseRelation.database(), 
					firstBaseRelation.aliases(),
					firstBaseRelation.condition(),
					softCondition,
					firstBaseRelation.joinConditions(), 
					projections, 
					allUnique, longestOrderSpecs, firstBaseRelation.limit(), firstBaseRelation.limitInverse());
		}
	}

	public Collection<BindingMaker> bindingMakers() {
		Collection<BindingMaker> results = new ArrayList<BindingMaker>();
		if (relationCounter == 1 || !differentConditions) {
			// Return list of unchanged triple makers
			for (BiningMakerAndCondition maker: makers) {
				if (maker.bMaker == null) continue;
				results.add(maker.bMaker);
			}
		} else {
			// Make binding makers conditional on the added boolean condition
			for (BiningMakerAndCondition maker: makers) {
				if (maker.bMaker == null) continue;
				if (maker.condition.isTrue()) {
					results.add(maker.bMaker);
				} else {
					results.add(maker.makeConditional());
				}
			}
		}
		return results;
	}
	
	private class BiningMakerAndCondition {
		private final BindingMaker bMaker;
		private final Expression condition;
		private final Expression softCondition;
		BiningMakerAndCondition(BindingMaker maker, Expression condition,
				Expression softCondition) {
			this.bMaker = maker;
			this.condition = condition;
			this.softCondition = softCondition;
		}
		private ProjectionSpec conditionProjection() {
			return new ExpressionProjectionSpec(
					firstBaseRelation.database().vendor().booleanExpressionToSimpleExpression(condition));
		}
		private BindingMaker makeConditional() {
			return bMaker.makeConditional(conditionProjection());
		}
		private Expression conditionWithSoft() {
			return condition.and(softCondition);
		}
	}
}
