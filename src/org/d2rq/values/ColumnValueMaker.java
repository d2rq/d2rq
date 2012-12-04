package org.d2rq.values;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.ProjectionSpec.ColumnProjectionSpec;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.nodes.NodeSetFilter;


/**
 * A {@link ValueMaker} that takes its values from a single
 * column.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ColumnValueMaker implements ValueMaker {
	private ColumnName column;
	private ProjectionSpec projection;
	private Set<ProjectionSpec> asSet;
	
	public ColumnValueMaker(ColumnName column) {
		this.column = column;
		projection = ColumnProjectionSpec.create(column);
		asSet = Collections.<ProjectionSpec>singleton(projection);
	}
	
	public String makeValue(ResultRow row) {
		return row.get(projection);
	}

	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToColumn(column);
	}

	public boolean matches(String value) {
		return true;
	}

	public Expression valueExpression(String value, DatabaseOp tabular, Vendor vendor) {
		if (value == null) {
			return Expression.FALSE;
		}
		return Equality.createColumnValue(column, value, 
				tabular.getColumnType(column));
	}

	public Set<ProjectionSpec> projectionSpecs() {
		return asSet;
	}

	public ValueMaker rename(Renamer renamer) {
		return new ColumnValueMaker(renamer.applyTo(column));
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		return Collections.singletonList(
				new OrderSpec(new ColumnExpr(column), ascending));
	}

	public String toString() {
		return "Column(" + column + ")";
	}
}
