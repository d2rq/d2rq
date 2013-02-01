package org.d2rq.db.op.util;

import java.util.Stack;

import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.OpVisitor;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.ProjectionSpec.ExprProjectionSpec;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.schema.ColumnName;

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

	public static Expression getDerivedColumnExpression(final DatabaseOp op, final ColumnName column) {
		if (!op.hasColumn(column)) return null;
		return new OpVisitor.Default(true) {
			private Expression result = null;
			Expression getResult() {
				op.accept(this);
				return result;
			}
			@Override
			public boolean visitEnter(ProjectOp table) {
				for (ProjectionSpec spec: table.getProjections()) {
					// TODO: instanceof is ugly
					if (spec.getColumn().equals(column) && spec instanceof ExprProjectionSpec) {
						result = ((ExprProjectionSpec) spec).getExpression();
						return false;
					}
				}
				return true;
			}
			@Override
			public boolean visitEnter(AliasOp table) {
				// Doesn't count as computed column
				result = null;
				return false;
			}
		}.getResult();
	}
	
	/**
	 * Check if the column is known to have the same value in all rows.
	 * This is the case for column X in queries like this:
	 * 
	 * SELECT X FROM T WHERE X=5 AND Y>0
	 * SELECT 5 AS X
	 * 
	 * The implementation walks through the operator tree, assuming by default
	 * that columns are not constant, and looking for possible reasons why they
	 * might. If such a reason is found, further recursion is stopped.
	 * 
	 * @param op
	 * @param column
	 * @return
	 */
	public static boolean isConstantColumn(final DatabaseOp op, final ColumnName column) {
		if (!op.hasColumn(column)) return true;	// by logic that it's safe to assume constant NULL
		return new OpVisitor.Default(true) {
			private boolean result = false;
			boolean getResult() {
				op.accept(this);
				return result;
			}
			@Override
			public boolean visitEnter(ProjectOp table) {
				// Is our column a derived column defined for the SELECT clause,
				// rather than a table column? If so, check if the expression
				// is constant
				Expression colExpression = getDerivedColumnExpression(op, column);
				if (colExpression != null && colExpression.isConstant()) {
					result = true;
					return false;
				}
				return true;
			}
			@Override
			public boolean visitEnter(SelectOp table) {
				// Does the condition force our column to be constant?
				if (table.getCondition().isConstantColumn(column, true, false, false)) {
					result = true;
					return false;
				}
				return true;
			}
			@Override
			public boolean visitEnter(AliasOp table) {
				// Check the original column
				result = isConstantColumn(table.getOriginal(), 
						table.getOriginalColumnName(column));
				return false;
			}
		}.getResult();
	}
	
	/**
	 * Cannot be instantiated, just static methods.
	 */
	private OpUtil() {}
}
