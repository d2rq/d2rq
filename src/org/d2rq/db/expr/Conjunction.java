package org.d2rq.db.expr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;


public class Conjunction extends NAryOperator {

	public static Expression create(Expression... expressions) {
		return Conjunction.create(Arrays.asList(expressions));
	}
	
	public static Expression create(Collection<Expression> expressions) {
		Set<Expression> elements = new HashSet<Expression>(expressions.size());
		for (Expression expression: expressions) {
			if (expression.isFalse()) {
				return Expression.FALSE;
			}
			if (expression.isTrue()) {
				continue;
			}
			if (expression instanceof Conjunction) {
				Conjunction conjunction = (Conjunction) expression;
				elements.addAll(Arrays.asList(conjunction.getOperands()));
			} else {
				elements.add(expression);
			}
		}
		if (elements.isEmpty()) {
			return Expression.TRUE;
		}
		if (elements.size() == 1) {
			return (Expression) elements.iterator().next();
		}
		return new Conjunction(elements.toArray(new Expression[elements.size()]));
	}
	
	private Conjunction(Expression[] expressions) {
		super("AND", "Conjunction", expressions, true, GenericType.BOOLEAN);
	}
	
	@Override
	protected Expression clone(Expression[] newParts) {
		return new Conjunction(newParts);
	}
	
	@Override
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue,
			boolean constIfFalse, boolean constIfConstantValue) {
		if (!constIfTrue) return false;
		for (Expression expression: getOperands()) {
			if (expression.isConstantColumn(column, true, false, false)) return true;
		}
		return false;
	}
}
