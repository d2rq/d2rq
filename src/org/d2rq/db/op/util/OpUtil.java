package org.d2rq.db.op.util;

import java.util.Stack;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.OpVisitor;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;

/**
 * Various utility functions for working with {@link DatabaseOp} instances.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class OpUtil {

	/**
	 * @return <code>true</code> if the table is known to have zero rows
	 */
	public static boolean isEmpty(final DatabaseOp tabular) {
		if (tabular == null) return true;
		return new OpVisitor.Default(true) {
			boolean result = false;
			boolean getResult() {
				tabular.accept(this);
				return result;
			}
			@Override
			public boolean visitEnter(SelectOp table) {
				if (table.getCondition().isFalse()) result = true;
				return result == false;
			}
			@Override
			public boolean visitEnter(LimitOp table) {
				if (table.getLimit() == 0) result = true;
				return result == false;
			}
			@Override
			public boolean visitEnter(EmptyOp table) {
				result = true;
				return false;
			}
		}.getResult();
	}
	
	/**
	 * @return <code>true</code> if the table has one row with no columns
	 */
	public static boolean isTrivial(final DatabaseOp tabular) {
		return new OpVisitor.Default(true) {
			final Stack<Boolean> resultStack = new Stack<Boolean>();
			boolean getResult() {
				tabular.accept(this);
				return resultStack.pop();
			}
			@Override
			public void visitLeave(InnerJoinOp table) {
				boolean result = true;
				for (boolean oneChildResult: resultStack) {
					result = result && oneChildResult;
				}
				resultStack.clear();
				resultStack.push(result);
			}
			@Override
			public void visitLeave(SelectOp table) {
				resultStack.push(resultStack.pop() && table.getCondition().isTrue());
			}
			@Override
			public void visitLeave(ProjectOp table) {
				resultStack.push(resultStack.pop() && table.getProjections().isEmpty());
			}
			@Override
			public void visitLeave(EmptyOp table) {
				resultStack.pop();
				resultStack.push(false);
			}
			@Override
			public void visit(TableOp table) {
				resultStack.push(false);
			}
			@Override
			public void visit(SQLOp table) {
				resultStack.push(false);
			}
			@Override
			public void visitOpTrue() {
				resultStack.push(true);
			}
		}.getResult();
	}
	
	/**
	 * Cannot be instantiated, just static methods.
	 */
	private OpUtil() {}
}
