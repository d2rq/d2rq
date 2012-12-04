package org.d2rq.db.op;

import org.d2rq.db.expr.Expression;


public class SelectOp extends DatabaseOp.Wrapper {
	
	public static DatabaseOp select(DatabaseOp original, Expression condition) {
		if (condition.isTrue()) {
			return original;
		}
		if (condition.isFalse()) {
			return original.getColumns().isEmpty() ? EmptyOp.NO_COLUMNS : EmptyOp.create(original);
		}
		return new SelectOp(condition, original);
	}
	
	private final Expression condition;
	
	private SelectOp(Expression condition, DatabaseOp wrapped) {
		super(wrapped);
		this.condition = condition;
	}
	
	public Expression getCondition() {
		return condition;
	}
	
	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			getWrapped().accept(visitor);
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "Selection(" + condition + "," + getWrapped() + ")"; 
	}
	
	@Override
	public int hashCode() {
		return condition.hashCode() ^ getWrapped().hashCode() ^ 442;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SelectOp)) return false;
		SelectOp other = (SelectOp) o;
		return condition.equals(other.condition) && 
				getWrapped().equals(other.getWrapped());
	}
}
