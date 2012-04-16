package de.fuberlin.wiwiss.d2rq.expr;

import java.util.HashSet;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * An expression that is TRUE iff its two constituent expressions are true.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Equality extends Expression {

	public static Expression create(Expression expr1, Expression expr2) {
		if (expr1.equals(expr2)) {
			return Expression.TRUE;
		}
		return new Equality(expr1, expr2);
	}
	
	public static Expression createAttributeEquality(
			Attribute attribute1, Attribute attribute2) {
		return (attribute1.compareTo(attribute2) < 0)
				? create(new AttributeExpr(attribute1), new AttributeExpr(attribute2))
				: create(new AttributeExpr(attribute2), new AttributeExpr(attribute1));
	}

	public static Expression createAttributeValue(Attribute attribute, String value) {
		return create(new AttributeExpr(attribute), new Constant(value, attribute));
	}
	
	public static Expression createExpressionValue(Expression expression, String value) {
		return create(expression, new Constant(value));
	}
	
	private final Expression expr1;
	private final Expression expr2;
	private final Set<Attribute> columns = new HashSet<Attribute>();
	
	private Equality(Expression expr1, Expression expr2) {
		this.expr1 = expr1;
		this.expr2 = expr2;
		columns.addAll(expr1.attributes());
		columns.addAll(expr2.attributes());
	}
	
	public Set<Attribute> attributes() {
		return columns;
	}

	public boolean isFalse() {
		return (expr1.isFalse() && expr2.isTrue())
				|| (expr1.isTrue() && expr2.isFalse());
	}

	public boolean isTrue() {
		return expr1.equals(expr2);
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return new Equality(
				expr1.renameAttributes(columnRenamer), 
				expr2.renameAttributes(columnRenamer));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return expr1.toSQL(database, aliases) + " = " + expr2.toSQL(database, aliases);
	}
	
	public String toString() {
		return "Equality(" + expr1 + ", " + expr2 + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Equality)) {
			return false;
		}
		Equality otherEquality = (Equality) other;
		if (expr1.equals(otherEquality.expr1) && expr2.equals(otherEquality.expr2)) {
			return true;
		}
		if (expr1.equals(otherEquality.expr2) && expr2.equals(otherEquality.expr1)) {
			return true;
		}
		return false;
	}
	
	public int hashCode() {
		return expr1.hashCode() ^ expr2.hashCode();
	}
}
