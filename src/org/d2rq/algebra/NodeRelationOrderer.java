package org.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.util.OpMutator;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.TableRenamer;
import org.d2rq.nodes.BindingMaker;


/**
 * Orders a {@link NodeRelation} by one of its variables. This implies
 * re-ordering of the underyling {@link DatabaseOp}, which may require
 * wrapping into a sub-<code>SELECT</code> in some cases, for example
 * if the {@link DatabaseOp} has a <code>LIMIT</code>. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NodeRelationOrderer extends OpMutator {
	private final List<OrderSpec> orderSpecs;
	private BindingMaker bindingMaker;
	private final SQLConnection sqlConnection;
	
	public NodeRelationOrderer(NodeRelation nodeRelation, List<OrderSpec> orderSpecs) {
		super(nodeRelation.getBaseTabular());
		this.orderSpecs = orderSpecs;
		this.bindingMaker = nodeRelation.getBindingMaker();
		this.sqlConnection = nodeRelation.getSQLConnection();
	}

	public NodeRelation getNodeRelation() {
		return new NodeRelation(sqlConnection, getResult(), bindingMaker);
	}

	private DatabaseOp wrap(DatabaseOp child) {
		return new OrderOp(orderSpecs, child);
	}
	
	@Override
	public boolean visitEnter(InnerJoinOp original) {
		return false;
	}
	
	@Override
	public DatabaseOp visitLeave(InnerJoinOp original,
			Collection<NamedOp> newChildren) {
		return wrap(original);
	}
	
	@Override
	public boolean visitEnter(SelectOp original) {
		return false;
	}
	
	@Override
	public DatabaseOp visitLeave(SelectOp original, DatabaseOp child) {
		return wrap(original);
	}
	
	@Override
	public boolean visitEnter(AliasOp original) {
		return false;
	}
	
	@Override
	public DatabaseOp visitLeave(AliasOp original, DatabaseOp child) {
		return wrap(original);
	}
	
	@Override
	public boolean visitEnter(OrderOp original) {
		return false;
	}
	
	/**
	 * We can merge with an existing ORDER BY clause.
	 */
	@Override
	public DatabaseOp visitLeave(OrderOp original, DatabaseOp child) {
		List<OrderSpec> fullList = new ArrayList<OrderSpec>(orderSpecs);
		for (OrderSpec orderSpec: original.getOrderBy()) {
			if (!fullList.contains(orderSpec)) {
				fullList.add(orderSpec);
			}
		}
		return new OrderOp(fullList, original);
	}
	
	@Override
	public boolean visitEnter(LimitOp original) {
		return false;
	}

	/**
	 * We have a LIMIT clause; adding ORDER BY would change the result as
	 * ordering is done before LIMIT in SQL. So we have to wrap the entire
	 * thing into a sub-SELECT, order the results of that sub-SELECT, and
	 * rename everything in the node makers to use the new table name.
	 * 
	 * FIXME: Handle column name clashes like TABLE1.COL, TABLE2.COL which has to become ALIAS.TABLE1_COL, ALIAS.TABLE2_COL or something like that
	 */
	@Override
	public DatabaseOp visitLeave(LimitOp original, DatabaseOp child) {
		AliasOp alias = AliasOp.createWithUniqueName(original, "LIMIT");
		bindingMaker = bindingMaker.rename(TableRenamer.create(alias));
		return new OrderOp(orderSpecs, alias);
	}
	
	@Override
	public boolean visitEnter(EmptyOp original) {
		return false;
	}
	
	/**
	 * Sorting an empty table is a no-op, so just return the original
	 */
	@Override
	public DatabaseOp visitLeave(EmptyOp original, DatabaseOp child) {
		return original;
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
