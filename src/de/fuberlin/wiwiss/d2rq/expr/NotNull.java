package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class NotNull extends Expression {

	public static Expression create(Expression expr) {
		return new NotNull(expr);
	}
	
	private Expression expr;
	
	private NotNull(Expression expr) {
		this.expr = expr;
	}
	
	public Set<Attribute> attributes() {
		return expr.attributes();
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return NotNull.create(columnRenamer.applyTo(expr));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return expr.toSQL(database, aliases) + " IS NOT NULL";
	}
	
	public String toString() {
		return "NotNull(" + this.expr + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof NotNull)) {
			return false;
		}
		NotNull otherExpression = (NotNull) other;
		return expr.equals(otherExpression.expr); 
	}
	
	public int hashCode() {
		return this.expr.hashCode() ^ 58473;
	}
}
