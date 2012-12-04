package de.fuberlin.wiwiss.d2rq.optimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.d2rq.algebra.NodeRelation;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.expr.Disjunction;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.mutators.OpUtil;
import org.d2rq.nodes.BindingMaker;
import org.d2rq.nodes.NodeMaker;


/**
 * A group of {@link DatabaseOp}s that can be combined into a single
 * relation without changing the semantics of {@link NodeMaker}s.
 * 
 * Tabulars can be combined if they access the same database and 
 * only differ in their projections.
 * 
 * Relations that just differ in their <code>WHERE</code> clause (condition)
 * can still be combined, but require that the new relation has the disjunction
 * (<code>OR</code>) of all conditions as a <code>WHERE</code> clause, and
 * the individual conditions must be added to the <code>SELECT</code> list
 * (as {@link ProjectionSpec}s) so that we can generate bindings only
 * if that clause is <code>TRUE</code>.
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
		if (OpUtil.isEmpty(nodeRelation.getBaseTabular())) return;
		for (CompatibleRelationGroup group: groups) {
			if (group.isCompatible(nodeRelation.getBaseTabular())) {
				group.addNodeRelation(nodeRelation);
				return;
			}
		}
		CompatibleRelationGroup newGroup = new CompatibleRelationGroup();
		newGroup.addNodeRelation(nodeRelation);
		groups.add(newGroup);
	}
	
	private final List<BindingMakerAndCondition> makers = new ArrayList<BindingMakerAndCondition>();
	private DatabaseOp firstBaseRelation = null;
	private SQLConnection firstSQLConnection = null;
	private boolean differentConditions = false;
	private boolean differentSoftConditions = false;
	private boolean allUnique = true;
	private int relationCounter = 0;
	private Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
	private List<OrderSpec> longestOrderSpecs = new ArrayList<OrderSpec>();
	
	public boolean isCompatible(DatabaseOp otherRelation) {
		if (firstBaseRelation == null) {
			throw new IllegalStateException();
		}
		if (firstBaseRelation.database()==null || !firstBaseRelation.database().equals(otherRelation.database())) {
			return false;
		}
		if (!firstBaseRelation.joinConditions().equals(otherRelation.joinConditions())) {
			return false;
		}
		Set<DatabaseOp> firstTables = firstBaseRelation.tables();
		Set<DatabaseOp> secondTables = otherRelation.tables();
		if (!firstTables.equals(secondTables)) {
			return false;
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

	private void addTabular(DatabaseOp tabular, SQLConnection connection) {
		if (firstBaseRelation == null) {
			firstBaseRelation = tabular;
			firstSQLConnection = connection;
			longestOrderSpecs.addAll(firstBaseRelation.orderSpecs());
		}
		if (!tabular.getCondition().equals(firstBaseRelation.getCondition())) {
			differentConditions = true;
		}
		if (!tabular.softCondition().equals(firstBaseRelation.softCondition())) {
			differentSoftConditions = true;
		}
		projections.addAll(tabular.projections());
		allUnique = allUnique && tabular.isUnique();
		relationCounter++;
	}

	public void addNodeRelation(NodeRelation nodeRelation) {
		DatabaseOp tabular = nodeRelation.getBaseTabular();
		addTabular(tabular, nodeRelation.getSQLConnection());
		makers.add(new BindingMakerAndCondition(nodeRelation.getBindingMaker(), 
				tabular.getCondition(), tabular.softCondition()));
	}
	
	public SQLConnection getSQLConnection() {
		return firstSQLConnection;
	}
	
	public DatabaseOp baseRelation() {
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
			for (BindingMakerAndCondition maker: makers) {
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
					firstBaseRelation.tables(),
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
				for (BindingMakerAndCondition maker: makers) {
					allSoftConditions.add(maker.softCondition);
				}
				if (allSoftConditions.isEmpty()) {
					allSoftConditions.add(Expression.TRUE);
				}
				softCondition = Disjunction.create(allSoftConditions);
			}
			// return a new relation with all the projections
			return new RelationImpl(firstBaseRelation.database(), 
					firstBaseRelation.tables(),
					firstBaseRelation.aliases(),
					firstBaseRelation.getCondition(),
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
			for (BindingMakerAndCondition maker: makers) {
				if (maker.bMaker == null) continue;
				results.add(maker.bMaker);
			}
		} else {
			// Make binding makers conditional on the added boolean condition
			for (BindingMakerAndCondition maker: makers) {
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
	
	private class BindingMakerAndCondition {
		private final BindingMaker bMaker;
		private final Expression condition;
		private final Expression softCondition;
		BindingMakerAndCondition(BindingMaker maker, Expression condition,
				Expression softCondition) {
			this.bMaker = maker;
			this.condition = condition;
			this.softCondition = softCondition;
		}
		private ProjectionSpec conditionProjection() {
			return ProjectionSpec.create(
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
