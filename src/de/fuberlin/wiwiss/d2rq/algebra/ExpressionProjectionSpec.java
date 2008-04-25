package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class ExpressionProjectionSpec implements ProjectionSpec {
	private Expression expression;
	private final String name;
	
	public ExpressionProjectionSpec(Expression expression) {
		this.expression = expression;
		this.name = "expr" + Integer.toHexString(expression.hashCode());
	}
	
	public ProjectionSpec renameAttributes(ColumnRenamer renamer) {
		return new ExpressionProjectionSpec(renamer.applyTo(expression));
	}

	public Set requiredAttributes() {
		return expression.columns();
	}

	public Expression toExpression() {
		return expression;
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return expression.toSQL(database, aliases) + " AS " + name;
	}
	
	public boolean equals(Object other) {
		return (other instanceof ExpressionProjectionSpec) 
				&& expression.equals(((ExpressionProjectionSpec) other).expression);
	}
	
	public int hashCode() {
		return expression.hashCode() ^ 684036;
	}

	public String toString() {
		return "ProjectionSpec(" + expression + " AS " + name + ")";
	}
}