package de.fuberlin.wiwiss.d2rq.optimizer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.d2rq.algebra.NodeRelation;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.OrderSpec;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.mutators.OpMutator;
import org.d2rq.db.renamer.ColumnRenamerMap;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;
import org.d2rq.nodes.BindingMaker;



/**
 * <p>Removes unnecessary joins from the {@link InnerJoinOp}s in a
 * {@link NodeRelation} in cases where this is possible without
 * affecting the result. This is an optimization.</p>
 * 
 * <p>A Table T2 joined to T1 via join J with join condition 
 * <em>T1.c_1 = T2.c_1 && T1.c_2 = T2.c_2 && ...</em>
 * can be removed if these conditions hold:</p>
 * 
 * <ol>
 *   <li>The only join mentioning T2 is J.</li>
 *   <li>All columns of T2 that are selected or constrained or used in
 *     an expression occur in J's join condition.</li>
 *   <li>All values of <em>T1.c_n</em> are guaranteed to occur
 *     in <em>T2.c_n</em>, that is, there is a foreign key constraint
 *     on <em>T1.c_n</em>.</li>
 * </ol>
 * 
 * <p>In this case, T2 and J can be dropped, and all mentions of <em>T2.c_n</em>
 * can be replaced with <em>T1.c_n</em>.</p>
 * 
 * TODO: This only optimizes the topmost join. Optimize nested joins too
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class JoinOptimizer extends OpMutator {
	
	public static NodeRelation optimize(NodeRelation nodeRelation) {
		return new JoinOptimizer(nodeRelation).getNodeRelation();
	}
	
	private final NodeRelation relation;
	private final BindingMaker bindingMaker;
	private final Set<ColumnName> requiredColumns = new HashSet<ColumnName>();
	private Renamer renamer = Renamer.IDENTITY;
	
	/**
	 * Constructs a new JoinOptimizer.
	 * @param relation The TripleRelation to be optimized
	 */
	private JoinOptimizer(NodeRelation relation) {
		super(relation.getBaseTabular());
		this.relation = relation;
		bindingMaker = relation.getBindingMaker();
	}
	
	public NodeRelation getNodeRelation() {
		return new NodeRelation(getResult(), bindingMaker.rename(renamer));
	}
	
	@Override
	public boolean visitEnter(InnerJoinOp original) {
		for (ColumnListEquality join: original.getJoinConditions()) {
			requiredColumns.addAll(join.getColumns1().getColumns());
			requiredColumns.addAll(join.getColumns2().getColumns());
		}
		return false;
	}

	@Override
	public DatabaseOp visitLeave(InnerJoinOp original,
			Collection<DatabaseOp> newChildren) {
		Map<ColumnName,ColumnName> replacedColumns = new HashMap<ColumnName,ColumnName>();
		Set<DatabaseOp> requiredTables = new HashSet<DatabaseOp>(original.getTables());
		Set<ColumnListEquality> requiredJoins = new HashSet<ColumnListEquality>(original.getJoinConditions());
		for (ColumnListEquality condition: original.getJoinConditions()) {
			if (!isRemovableJoin(original, condition)) continue;
			if (condition.joinDirection() == ColumnListEquality.Direction.RIGHT && 
					isRemovableJoinSide(condition.getTableName1(), condition)) {
				requiredJoins.remove(condition);
				replacedColumns.putAll(replacementColumns(condition.getColumns1(), condition));
				requiredTables.remove(original.getTable(condition.getTableName1()));
			}
			if (condition.joinDirection() == ColumnListEquality.Direction.LEFT && 
					isRemovableJoinSide(condition.getTableName2(), condition)) {
				requiredJoins.remove(condition);
				replacedColumns.putAll(replacementColumns(condition.getColumns2(), condition));
				requiredTables.remove(original.getTable(condition.getTableName2()));
			}
		}
		// No joins could be removed.
		if (replacedColumns.isEmpty() && requiredTables.size() == original.getTables().size()) {
			return original;
		}
		// Some joins have been removed, so we need to build a new InnerJoin
		// and rename columns in the node makers.
		renamer = new ColumnRenamerMap(replacedColumns);
		return InnerJoinOp.join(requiredTables, requiredJoins, original.getSQLConnection());
	}

	@Override
	public boolean visitEnter(SelectOp original) {
		requiredColumns.addAll(original.getCondition().getColumns());
		return true;
	}

	@Override
	public DatabaseOp visitLeave(SelectOp original, DatabaseOp child) {
		return SelectOp.select(child, 
				renamer.applyTo(original.getCondition()));
	}

	@Override
	public boolean visitEnter(ProjectOp original) {
		for (ProjectionSpec projection: original.getProjections()) {
			requiredColumns.addAll(projection.toExpression().getColumns());
		}
		return true;
	}

	@Override
	public DatabaseOp visitLeave(ProjectOp original, DatabaseOp child) {
		return new ProjectOp(
				renamer.applyToProjections(original.getProjections()), child);
	}

	@Override
	public boolean visitEnter(AliasOp original) {
		// TODO Auto-generated method stub
		return super.visitEnter(original);
	}

	@Override
	public DatabaseOp visitLeave(AliasOp original, DatabaseOp child) {
		// TODO Auto-generated method stub
		return super.visitLeave(original, child);
	}

	@Override
	public boolean visitEnter(OrderOp original) {
		for (OrderSpec orderBy: original.getOrderBy()) {
			requiredColumns.addAll(orderBy.getExpression().getColumns());
		}
		return true;
	}

	@Override
	public DatabaseOp visitLeave(OrderOp original, DatabaseOp child) {
		return new OrderOp(renamer.applyTo(original.getOrderBy()), child);
	}

	@Override
	public boolean visitEnter(EmptyOp original) {
		return false;
	}

	@Override
	public DatabaseOp visitLeave(EmptyOp original, DatabaseOp child) {
		return original;
	}

	private boolean isRemovableJoin(InnerJoinOp join, ColumnListEquality condition) {
		for (ColumnName side1: condition.getColumns1().getColumns()) {
			ColumnName side2 = condition.getEqualColumn(side1);
			if (!relation.getBaseTabular().getSQLConnection().areCompatibleFormats(
					join.getTable(condition.getTableName1()), side1, 
					join.getTable(condition.getTableName2()), side2)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if the table on one side of a join is irrelevant to the result.
	 * @param tableName A table that is on one side of the join
	 * @param condition The join whose status we check
	 * @return <tt>true</tt> iff all columns from that table are covered by
	 * 		the join's condition
	 */
	private boolean isRemovableJoinSide(TableName tableName, ColumnListEquality condition) {
		for (ColumnName requiredColumn: requiredColumns) {
			// This shouldn't happen; participating Tabulars in a join must be
			// named, and hence their columns are qualified.
			if (!requiredColumn.isQualified()) continue;
			// Column is in another table?
			if (!requiredColumn.getQualifier().equals(tableName)) continue;
			// Column is in our table, but not covered in the join condition?
			// Then we cannot remove the table.
			if (!condition.containsColumn(requiredColumn)) return false;
		}
		// All columns from our table are in the join condition.
		return true;
	}
	
	private Map<ColumnName,ColumnName> replacementColumns(Key originalColumns, ColumnListEquality removableJoin) {
		Map<ColumnName,ColumnName> result = new HashMap<ColumnName,ColumnName>();
		for (ColumnName originalColumn: originalColumns) {
			result.put(originalColumn, removableJoin.getEqualColumn(originalColumn));
		}
		return result;
	}
}
