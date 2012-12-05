package org.d2rq.algebra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.OpVisitor;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.op.util.OpRenamer;
import org.d2rq.db.op.util.OpSelecter;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.renamer.TableRenamer;
import org.d2rq.db.schema.TableName;
import org.d2rq.nodes.BindingMaker;
import org.d2rq.nodes.FixedNodeMaker;
import org.d2rq.nodes.NodeMaker;
import org.d2rq.nodes.NodeMaker.EmptyNodeMaker;
import org.d2rq.nodes.NodeMakerVisitor;
import org.d2rq.nodes.TypedNodeMaker;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;


/**
 * Utility operations for manipulating {@link NodeRelation}s. Basically
 * provides a number of relational operators.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NodeRelationUtil {

	/**
	 * Joins this NodeRelation with a Binding. Any row in this
	 * NodeRelation that is incompatible with the binding will be
	 * dropped, and any compatible row will be extended with
	 * FixedNodeMakers whose node is taken from the binding.
	 * 
	 * FIXME: This doesn't behave correctly when a node maker is available for a given variable but produces unbound results. Everything is compatible with unbound.
	 * FIXME: This ignores the condition of the binding maker, if any is present.
	 * 
	 * @param binding A binding to join with this NodeRelation
	 * @return The joined NodeRelation
	 */
	public static NodeRelation extendWith(NodeRelation table, Binding binding) {
		if (binding.isEmpty()) return table;
		Map<Var,NodeMaker> extraVars = new HashMap<Var,NodeMaker>();
		NodeRelation result = table;
		for (Iterator<Var> it = binding.vars(); it.hasNext();) {
			Var var = it.next();
			Node value = binding.get(var);
			if (table.getBindingMaker().has(var)) {
				result = NodeRelationUtil.select(result, var, value);
			} else {
				extraVars.put(var, new FixedNodeMaker(value));
			}
		}
		if (!extraVars.isEmpty()) {
			extraVars.putAll(result.getBindingMaker().getNodeMakers());
			result = new NodeRelation(result.getSQLConnection(), result.getBaseTabular(), 
					new BindingMaker(extraVars, result.getBindingMaker().getCondition()));
		}
		return result;
	}

	// FIXME: This doesn't work correctly if a condition is present on the binding maker. Would need to create a sub-SELECT with the condition, and apply the limit to that.
	public static NodeRelation limit(NodeRelation table, int limit) {
		return new NodeRelation(table.getSQLConnection(), 
				LimitOp.limit(table.getBaseTabular(), limit, LimitOp.NO_LIMIT), 
				table.getBindingMaker());
	}

	public static NodeRelation order(NodeRelation nodeRelation, 
			Var orderByVar, boolean ascending) {
		if (!nodeRelation.getBindingMaker().has(orderByVar)) {
			return nodeRelation;
		}
		List<OrderSpec> orderSpecs = nodeRelation.nodeMaker(orderByVar).orderSpecs(ascending);
		if (orderSpecs.isEmpty()) {
			return nodeRelation;
		}
		return new NodeRelationOrderer(nodeRelation, orderSpecs).getNodeRelation();
	}
	
	public static NodeRelation project(NodeRelation original, Set<Var> vars) {
		Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
		for (Var var: vars) {
			if (!original.getBindingMaker().has(var)) continue;
			projections.addAll(original.nodeMaker(var).projectionSpecs());
		}
		return new NodeRelationProjecter(original, projections).getNodeRelation();
	}
	
	public static NodeRelation renameWithPrefix(NodeRelation table, final int index) {
		final Map<TableName,TableName> oldToNew = new HashMap<TableName,TableName>();
		table.getBaseTabular().accept(new OpVisitor.Default(true) {
			@Override
			public boolean visitEnter(AliasOp alias) {
				oldToNew.put(alias.getTableName(), alias.getTableName().withPrefix(index));
				return false;
			}
			@Override
			public void visit(TableOp table) {
				oldToNew.put(table.getTableName(), table.getTableName().withPrefix(index));
			}
		});
		Renamer renamer = TableRenamer.create(oldToNew);
		DatabaseOp renamed = new OpRenamer(table.getBaseTabular(), renamer) {
			@Override
			public DatabaseOp visit(TableOp table) {
				return AliasOp.create(table, oldToNew.get(table.getTableName()));
			}
		}.getResult();
		return new NodeRelation(table.getSQLConnection(), 
				renamed, table.getBindingMaker().rename(renamer));
	}
	
	// TODO: I think we shouldn't need this
	public static NodeRelation renameSingleRelation(NodeRelation table, 
			TableName oldName, TableName newName) {
		Renamer renamer = TableRenamer.create(oldName, newName);
		return new NodeRelation(table.getSQLConnection(),
				new OpRenamer(table.getBaseTabular(), renamer).getResult(), 
				table.getBindingMaker().rename(renamer));
	}

	// TODO: This should take an ARQ Expr as argument and transform it to an Expression
	public static NodeRelation select(NodeRelation original, Expression expression) {
		if (expression.isTrue()) {
			return original;
		}
		if (expression.isFalse()) {
			return NodeRelation.createEmpty(original);
		}
		return new NodeRelation(
				original.getSQLConnection(),
				new OpSelecter(original.getBaseTabular(), expression).getResult(), 
				original.getBindingMaker());
	}
	
	public static NodeRelation select(final NodeRelation original,
			final Var var, final Node value) {
		if (value.isVariable() || value == Node.ANY) {
			return original;
		}
		if (!original.getBindingMaker().has(var)) {
			return NodeRelation.createEmpty(original);
		}
		return new NodeMakerVisitor() {
			private NodeRelation result = original;
			public NodeRelation getResult() {
				original.nodeMaker(var).accept(this);
				return result;
			}
			public void visit(EmptyNodeMaker nodeMaker) {
				result = NodeRelation.createEmpty(original);
			}
			public void visit(FixedNodeMaker nodeMaker) {
				if (!nodeMaker.getFixedNode().equals(value)) {
					result = NodeRelation.createEmpty(original);
				}
			}
			public void visit(TypedNodeMaker nodeMaker) {
				if (!nodeMaker.getNodeType().matches(value)) {
					result = NodeRelation.createEmpty(original);
					return;
				}
				String stringValue = nodeMaker.getNodeType().extractValue(value);
				if (stringValue == null) {
					result = NodeRelation.createEmpty(original);
					return;
				}
				Expression expr = nodeMaker.getValueMaker().valueExpression(
						stringValue, original.getBaseTabular(), original.getSQLConnection().vendor());
				if (expr.isFalse()) {
					result = NodeRelation.createEmpty(original);
					return;
				}
				Map<Var,NodeMaker> nodeMakers = new HashMap<Var,NodeMaker>(
						original.getBindingMaker().getNodeMakers());
				nodeMakers.put(var, new FixedNodeMaker(value));
				result = new NodeRelation(
						result.getSQLConnection(),
						new OpSelecter(original.getBaseTabular(), expr).getResult(), 
						new BindingMaker(nodeMakers, original.getBindingMaker().getCondition()));
			}
		}.getResult();
	}
	
	private NodeRelationUtil() {
		// Can't be instantiated, only static methods
	}
}
