package org.d2rq.values;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.nodes.NodeSetFilter;


/**
 * A value maker that creates its values from a SQL expression.
 * 
 * TODO Write unit tests
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQLExpressionValueMaker implements ValueMaker {
	private final Expression expression;
	private final ProjectionSpec projection;
	private final Vendor vendor;
	
	public SQLExpressionValueMaker(Expression expression, Vendor vendor) {
		this.expression = expression;
		this.projection = ProjectionSpec.create(expression, vendor);
		this.vendor = vendor;
	}
	
	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToExpression(expression);
	}
	
	public Set<ProjectionSpec> projectionSpecs() {
		return Collections.singleton(projection);
	}
	
	public String makeValue(ResultRow row) {
		return row.get(projection);
	}

	public boolean matches(String value) {
		return true;
	}
	
	public Expression valueExpression(String value, DatabaseOp table, Vendor vendor) {
		return Equality.createExpressionValue(expression, value, expression.getDataType(table, vendor));
	}
	
	public ValueMaker rename(Renamer renamer) {
		return new SQLExpressionValueMaker(renamer.applyTo(expression), vendor);
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		return Collections.singletonList(new OrderSpec(expression, ascending));
	}

	public int hashCode() {
		return expression.hashCode() ^ 5835034;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof SQLExpressionValueMaker)) {
			return false;
		}
		return expression.equals(((SQLExpressionValueMaker) other).expression);
	}
	
	public String toString() {
		return "SQLExpression(" + expression + ")";
	}
}
