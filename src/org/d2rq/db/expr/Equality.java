package org.d2rq.db.expr;

import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;


/**
 * An expression that is TRUE iff its two constituent expressions are true.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Equality extends BinaryOperator {

	public static Expression create(Expression expr1, Expression expr2) {
		if (expr1.equals(expr2)) {
			return Expression.TRUE;
		}
		return new Equality(expr1, expr2);
	}
	
	public static Expression createColumnEquality(
			ColumnName column1, ColumnName column2) {
		return (column1.compareTo(column2) < 0)
				? create(new ColumnExpr(column1), new ColumnExpr(column2))
				: create(new ColumnExpr(column2), new ColumnExpr(column1));
	}

	public static Expression createColumnValue(
			ColumnName column, String value, DataType dataType) {
		return create(new ColumnExpr(column), Constant.create(value, dataType));
	}
	
	public static Expression createExpressionValue(
			Expression expression, String value, DataType dataType) {
		return create(expression, Constant.create(value, dataType));
	}
	
	private final Set<ColumnName> columns = new HashSet<ColumnName>();
	
	private Equality(Expression expr1, Expression expr2) {
		super(expr1, expr2, "=", true, GenericType.BOOLEAN);
		columns.addAll(expr1.getColumns());
		columns.addAll(expr2.getColumns());
	}
	
	public boolean isFalse() {
		return (expr1.isFalse() && expr2.isTrue())
				|| (expr1.isTrue() && expr2.isFalse());
	}

	public boolean isTrue() {
		return expr1.equals(expr2);
	}

	public Expression rename(Renamer columnRenamer) {
		return new Equality(
				expr1.rename(columnRenamer), 
				expr2.rename(columnRenamer));
	}
}
