package org.d2rq.algebra;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.DistinctOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.op.util.OpMutator;
import org.d2rq.nodes.BindingMaker;


/**
 * Projects a {@link NodeRelation}. 
 * 
 * Projections are pushed down over {@link LimitOp} and
 * {@link EmptyOp}, merged with other {@link ProjectOp}s,
 * done over a sub-<code>SELECT</code> for {@link DistinctOp}s,
 * and simply wrapped around all other kinds of {@link DatabaseOp}s.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NodeRelationProjecter extends OpMutator {
	private final SQLConnection sqlConnection;
	private Set<ProjectionSpec> projections;
	private BindingMaker bindingMaker;
	
	public NodeRelationProjecter(NodeRelation original, Set<ProjectionSpec> projections) {
		super(original.getBaseTabular());
		this.projections = projections;
		this.bindingMaker = original.getBindingMaker();
		this.sqlConnection = original.getSQLConnection();
	}
	
	public NodeRelation getNodeRelation() {
		return new NodeRelation(sqlConnection, getResult(), bindingMaker);
	}

	private DatabaseOp wrap(DatabaseOp original) {
		return ProjectOp.create(original, projections);
	}
	
	@Override
	public boolean visitEnter(InnerJoinOp original) {
		return false;
	}

	/**
	 * TODO: Can we push parts of the projection down into the join?
	 *       E.g., can we eliminate one of the tables in the join,
	 *       or remove a column from a sub-select?
	 */ 
	@Override
	public DatabaseOp visitLeave(InnerJoinOp original,
			Collection<NamedOp> newChildren) {
		return wrap(original);
	}

	@Override
	public boolean visitEnter(SelectOp original) {
		return false;
	}

	/**
	 * TODO: Can we push the projection down over the selection?
	 *       All the columns needed for the selection must be present in the
	 *       projection. This can be beneficial if the projection can be
	 *       pushed even further down, e.g., over a join. 
	 */
	@Override
	public DatabaseOp visitLeave(SelectOp original, DatabaseOp child) {
		return wrap(original);
	}

	@Override
	public boolean visitEnter(ProjectOp original) {
		return false;
	}

	/**
	 * Merge the projection lists.
	 */
	@Override
	public DatabaseOp visitLeave(ProjectOp original, DatabaseOp child) {
		Set<ProjectionSpec> mergedProjections = 
			new HashSet<ProjectionSpec>(projections);
		projections.addAll(original.getProjections());
		return ProjectOp.create(original.getWrapped(), mergedProjections);
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
		return false;
	}

	/**
	 * TODO: Can we push the projection down over the ordering?
	 *       All the columns needed for the order spec must be present in
	 *       the projection. This can be beneficial if the projection can be
	 *       pushed even further down, e.g., over a join.
	 */
	@Override
	public DatabaseOp visitLeave(OrderOp original, DatabaseOp child) {
		return wrap(original);
	}

	@Override
	public boolean visitEnter(DistinctOp original) {
		return false;
	}

	/**
	 * We have a DISTINCT clause; adding or removing columns would change the
	 * result. So we have to wrap the entire
	 * thing into a sub-SELECT, apply DISTINCT on the sub-SELECT, and
	 * rename everything in the node makers to use the new table name.
	 */
	@Override
	public DatabaseOp visitLeave(DistinctOp original, DatabaseOp child) {
		AliasOp alias = AliasOp.createWithUniqueName(original, "PROJECT");
		bindingMaker = bindingMaker.rename(alias.getRenamer());
		projections = new HashSet<ProjectionSpec>(alias.getRenamer().applyToProjections(projections));
		return ProjectOp.create(alias, projections);
	}

	@Override
	public DatabaseOp visit(TableOp original) {
		return wrap(original);
	}

	@Override
	public DatabaseOp visit(SQLOp original) {
		return wrap(original);
	}

	@Override
	public DatabaseOp visitOpTrue() {
		return wrap(DatabaseOp.TRUE);
	}
}
