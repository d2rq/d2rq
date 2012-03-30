package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;
import java.util.List;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class OrderSpec {
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
	
	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return expression.toSQL(database, aliases) + (ascending ? "" : " DESC");
	}
	
	public Expression expression() {
		return expression;
	}
	
	public boolean isAscending() {
		return ascending;
	}
	
	public String toString() {
		return (ascending ? "ASC(" : "DESC(") + expression + ")";
	}
	
	public boolean equals(Object other) {
		if (other instanceof OrderSpec) {
			return ascending == ((OrderSpec) other).ascending && 
					expression.equals(((OrderSpec) other).expression);
		}
		return false;
	}
	
	public int hashCode() {
		return Boolean.valueOf(ascending).hashCode() ^ expression.hashCode();
	}
}
