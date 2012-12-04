package org.d2rq.db.op;

import java.util.Collections;
import java.util.List;

import org.d2rq.db.expr.Expression;

/**
 * An <code>ORDER BY</code> clause for a {@link DatabaseOp}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class OrderOp extends DatabaseOp.Wrapper {
	private final List<OrderSpec> orderBy;

	public OrderOp(List<OrderSpec> orderBy, DatabaseOp wrapped) {
		super(wrapped);
		this.orderBy = orderBy == null ? OrderSpec.NONE : orderBy;
	}

	public List<OrderSpec> getOrderBy() {
		return orderBy;
	}
	
	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			getWrapped().accept(visitor);
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "Order(" + getWrapped() + "," + orderBy + ")";
	}
	
	@Override
	public int hashCode() {
		return getWrapped().hashCode() ^ orderBy.hashCode() ^ 69;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof OrderOp)) return false;
		OrderOp other = (OrderOp) o;
		if (!getWrapped().equals(other.getWrapped())) return false;
		return orderBy.equals(other.orderBy);
	}

	public static class OrderSpec {
		public final static List<OrderSpec> NONE = Collections.emptyList();
		
		private Expression expression;
		private boolean ascending;
		
		public OrderSpec(Expression expression) {
			this(expression, true);
		}
		
		public OrderSpec(Expression expression, boolean ascending) {
			this.expression = expression;
			this.ascending = ascending;
		}
		
		public Expression getExpression() {
			return expression;
		}
		
		public boolean isAscending() {
			return ascending;
		}

		@Override
		public String toString() {
			return (ascending ? "ASC(" : "DESC(") + expression + ")";
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof OrderSpec) {
				return ascending == ((OrderSpec) other).ascending && 
						expression.equals(((OrderSpec) other).expression);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Boolean.valueOf(ascending).hashCode() ^ expression.hashCode() ^ 45;
		}
	}
}
