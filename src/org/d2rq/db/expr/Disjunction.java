package org.d2rq.db.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


public class Disjunction extends Expression {

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
				elements.addAll(((Disjunction) expression).expressions);
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
		return new Disjunction(elements);
	}
	
	public static Expression create(Expression expr1, Expression expr2) {
		if (expr1.equals(expr2)) return expr1;
		if (expr1.isTrue() || expr2.isTrue()) return Expression.TRUE;
		if (expr1.isFalse()) {
			if (expr2.isFalse()) return Expression.FALSE;
			return expr2;
		}
		if (expr2.isFalse()) return expr1;
		Set<Expression> expr = new HashSet<Expression>(2);
		expr.add(expr1);
		expr.add(expr2);
		return new Disjunction(expr);
	}
	
	private Set<Expression> expressions;
	private Set<ColumnName> columns = new HashSet<ColumnName>();
	
	private Disjunction(Set<Expression> expressions) {
		this.expressions = expressions;
		for (Expression expression: expressions) {
			this.columns.addAll(expression.getColumns());
		}
	}

	public boolean isTrue() {
		return false;
	}

	public boolean isFalse() {
		return false;
	}
	
	public boolean isConstant() {
		boolean onlyConstants = true;
		for (Expression expression: expressions) {
			if (!expression.isConstant()) onlyConstants = false;
			if (expression.isTrue()) return true;
		}
		return onlyConstants;
	}

	public boolean isConstantColumn(ColumnName column, boolean constIfTrue,
			boolean constIfFalse, boolean constIfConstantValue) {
		if (!constIfFalse) return false;
		for (Expression expression: expressions) {
			if (expression.isConstantColumn(column, false, true, false)) return true;
		}
		return false;
	}

	public Set<ColumnName> getColumns() {
		return this.columns;
	}

	public Expression rename(Renamer columnRenamer) {
		Set<Expression> renamedExpressions = new HashSet<Expression>();
		for (Expression expression: expressions) {
			renamedExpressions.add(expression.rename(columnRenamer));
		}
		return Disjunction.create(renamedExpressions);
	}

	public String toSQL(DatabaseOp table, Vendor vendor) {
		List<String> fragments = new ArrayList<String>(expressions.size());
		for (Expression expression: expressions) {
			fragments.add(expression.toSQL(table, vendor));
		}
		Collections.sort(fragments);
		StringBuffer result = new StringBuffer("(");
		Iterator<String> it = fragments.iterator();
		while (it.hasNext()) {
			String fragment = it.next();
			result.append(fragment);
			if (it.hasNext()) {
				result.append(" OR ");
			}
		}
		result.append(")");
		return result.toString();
	}

	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return GenericType.BOOLEAN.dataTypeFor(vendor);
	}
	
	public String toString() {
		List<String> fragments = new ArrayList<String>(expressions.size());
		for (Expression expression: expressions) {
			fragments.add(expression.toString());
		}
		Collections.sort(fragments);
		StringBuffer result = new StringBuffer("Disjunction(");
		Iterator<String> it = fragments.iterator();
		while (it.hasNext()) {
			String fragment = it.next();
			result.append(fragment);
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Disjunction)) {
			return false;
		}
		Disjunction otherConjunction = (Disjunction) other;
		return this.expressions.equals(otherConjunction.expressions);
	}
	
	public int hashCode() {
		return this.expressions.hashCode();
	}
}