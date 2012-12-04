package org.d2rq.db.op.util;

import java.util.Collection;

import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;


/**
 * Applies an {@link Expression} to a {@link DatabaseOp}. 
 * 
 * Wraps atomic tables and joins into {@link SelectOp}s, merges
 * {@link SelectOp}s into one, and recurses into other kinds of tabular wrappers.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class OpSelecter extends OpMutator {
	private final Expression expression;
	
	public OpSelecter(DatabaseOp table,
			Expression expression) {
		super(table);
		this.expression = expression;
	}

	private DatabaseOp wrap(DatabaseOp original) {
		return SelectOp.select(original, expression);
	}
	
	// TODO: Check if the condition can be distributed over the tables in the join
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
		return SelectOp.select(original, expression.and(original.getCondition()));
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
	public boolean visitEnter(EmptyOp original) {
		return false;
	}

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
