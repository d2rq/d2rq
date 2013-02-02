package org.d2rq.db.expr;

import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


/**
 * A CASE statement that turns a BOOLEAN (TRUE, FALSE) into an
 * INT (1, 0)
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class BooleanToIntegerCaseExpression extends UnaryExpression {
	
	public BooleanToIntegerCaseExpression(Expression operand) {
		super("Boolean2Int", operand, GenericType.NUMERIC);
	}

	@Override
	public Expression clone(Expression newOperand) {
		return new BooleanToIntegerCaseExpression(newOperand);
	}

	@Override
	public String toSQL(String operandSQL, Vendor vendor) {
		return "(CASE WHEN (" + operandSQL + ") THEN 1 ELSE 0 END)";
	}
}