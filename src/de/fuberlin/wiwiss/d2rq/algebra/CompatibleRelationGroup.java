package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.expr.Disjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.find.TripleMaker;

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
 * (as {@link ProjectionSpec}s) so that triples/bindings are generated only
 * if that clause is <code>TRUE</code>
 * 
 * TODO: The BindingRelation and TripleRelation stuff is virtually identical
 * TODO: Should check if the BindingMaker/TripleMaker already has a condition?
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class CompatibleRelationGroup {

	public static Collection<CompatibleRelationGroup> groupTripleRelations(
			Collection<TripleRelation> tripleRelations) {
		Collection<CompatibleRelationGroup> result = new ArrayList<CompatibleRelationGroup>();
		for (TripleRelation tripleRelation: tripleRelations) {
			addTripleRelation(tripleRelation, result);
		}
		return result;
	}
	
	private static void addTripleRelation(TripleRelation tripleRelation, 
			Collection<CompatibleRelationGroup> groups) {
		for (CompatibleRelationGroup group: groups) {
			if (group.isCompatible(tripleRelation.baseRelation())) {
				group.addTripleMaker(
						tripleRelation.baseRelation(), new TripleMaker(tripleRelation));
				return;
			}
		}
		CompatibleRelationGroup newGroup = new CompatibleRelationGroup();
		newGroup.addTripleMaker(
				tripleRelation.baseRelation(), new TripleMaker(tripleRelation));
		groups.add(newGroup);
	}
	
	public static Collection<CompatibleRelationGroup> groupNodeRelations(
			Collection<NodeRelation> nodeRelations) {
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
						nodeRelation.baseRelation(), new BindingMaker(nodeRelation));
				return;
			}
		}
		CompatibleRelationGroup newGroup = new CompatibleRelationGroup();
		newGroup.addBindingMaker(
				nodeRelation.baseRelation(), new BindingMaker(nodeRelation));
		groups.add(newGroup);
	}
	
	private final List<Maker> makers = new ArrayList<Maker>();
	private Relation firstBaseRelation = null;
	private boolean differentConditions = false;
	private boolean allUnique = true;
	private int relationCounter = 0;
	private Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
	
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
		if (firstBaseRelation.projections().equals(otherRelation.projections())) {
			return true;
		}
		if (firstBaseRelation.isUnique() && otherRelation.isUnique()) {
			return true;
		}
		return false;
	}

	private void addRelation(Relation relation) {
		if (firstBaseRelation == null) {
			firstBaseRelation = relation;
		}
		if (!relation.condition().equals(firstBaseRelation.condition())) {
			differentConditions = true;
		}
		projections.addAll(relation.projections());
		allUnique = allUnique && relation.isUnique();
		relationCounter++;
	}

	public void addTripleMaker(Relation relation, TripleMaker tripleMaker) {
		addRelation(relation);
		makers.add(new Maker(tripleMaker, relation.condition()));
	}
	
	public void addBindingMaker(Relation relation, BindingMaker bindingMaker) {
		addRelation(relation);
		makers.add(new Maker(bindingMaker, relation.condition()));
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
			for (Maker maker: makers) {
				if (maker.condition.isTrue()) continue;
				allConditions.add(maker.condition);
				projectionsAndConditions.add(maker.conditionProjection());
			}
			if (allConditions.isEmpty()) {
				allConditions.add(Expression.TRUE);
			}
			Disjunction.create(allConditions);
			return new RelationImpl(firstBaseRelation.database(),
					firstBaseRelation.aliases(),
					Disjunction.create(allConditions), 
					firstBaseRelation.joinConditions(), 
					projectionsAndConditions, 
					allUnique, firstBaseRelation.order(), firstBaseRelation.orderDesc(), firstBaseRelation.limit(), firstBaseRelation.limitInverse());
		} else {
			// Multiple relations with same condition, return a
			// new relation with all the projections
			return new RelationImpl(firstBaseRelation.database(), 
					firstBaseRelation.aliases(),
					firstBaseRelation.condition(), 
					firstBaseRelation.joinConditions(), 
					projections, 
					allUnique, firstBaseRelation.order(), firstBaseRelation.orderDesc(), firstBaseRelation.limit(), firstBaseRelation.limitInverse());
		}
	}
	
	public Collection<TripleMaker> tripleMakers() {
		Collection<TripleMaker> results = new ArrayList<TripleMaker>();
		if (relationCounter == 1 || !differentConditions) {
			// Return list of unchanged triple makers
			for (Maker maker: makers) {
				if (maker.tMaker == null) continue;
				results.add(maker.tMaker);
			}
		} else {
			// Make triple makers conditional on the added boolean condition
			for (Maker maker: makers) {
				if (maker.tMaker == null) continue;
				if (maker.condition.isTrue()) {
					results.add(maker.tMaker);
				} else {
					results.add(new TripleMaker(maker.tMaker, maker.conditionProjection()));
				}
			}
		}
		return results;
	}
	
	public Collection<BindingMaker> bindingMakers() {
		Collection<BindingMaker> results = new ArrayList<BindingMaker>();
		if (relationCounter == 1 || !differentConditions) {
			for (Maker maker: makers) {
				if (maker.bMaker == null) continue;
				results.add(maker.bMaker);
			}
		} else {
			for (Maker maker: makers) {
				if (maker.bMaker == null) continue;
				if (maker.condition.isTrue()) {
					results.add(maker.bMaker);
					results.add(new BindingMaker(maker.bMaker, maker.conditionProjection()));
				}
			}
		}
		return results;
	}
	
	private class Maker {
		private final BindingMaker bMaker;
		private final TripleMaker tMaker;
		private final Expression condition;
		Maker(BindingMaker maker, Expression condition) {
			this.bMaker = maker;
			this.tMaker = null;
			this.condition = condition;
		}
		Maker(TripleMaker maker, Expression condition) {
			this.bMaker = null;
			this.tMaker = maker;
			this.condition = condition;
		}
		ProjectionSpec conditionProjection() {
			return new ExpressionProjectionSpec(condition);
		}
	}
}
