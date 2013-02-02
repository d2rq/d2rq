package org.d2rq.db.expr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;


public class Disjunction extends NAryOperator {

	public static Expression create(Expression... operands) {
		return Disjunction.create(Arrays.asList(operands));
	}

	public static Expression create(Collection<Expression> expressions) {
		Set<Expression> elements = new HashSet<Expression>(expressions.size());
		for (Expression expression: expressions) {
			if (expression.isTrue()) {
				return Expression.TRUE;
			}
			if (expression.isFalse()) {
				continue;
			}
			if (expression instanceof Disjunction) {
				Disjunction disjunction = (Disjunction) expression;
				elements.addAll(Arrays.asList(disjunction.getOperands()));
			} else {
				elements.add(expression);
			}
		}
		if (elements.isEmpty()) {
			return Expression.FALSE;
		}
		if (elements.size() == 1) {
			return (Expression) elements.iterator().next();
		}
		return new Disjunction(elements.toArray(new Expression[elements.size()]));
	}
	
	private Disjunction(Expression[] operands) {
		super("OR", "Disjunction", operands, true, GenericType.BOOLEAN);
	}
	
	@Override
	protected Expression clone(Expression[] newParts) {
		return new Disjunction(newParts);
	}

	@Override
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue,
			boolean constIfFalse, boolean constIfConstantValue) {
		if (!constIfFalse) return false;
		for (Expression expression: getOperands()) {
			if (expression.isConstantColumn(column, false, true, false)) return true;
		}
		return false;
	}
}