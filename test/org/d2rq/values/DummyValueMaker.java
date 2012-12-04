package org.d2rq.values;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.nodes.NodeSetFilter;
import org.d2rq.values.ValueMaker;


/**
 * Dummy implementation of {@link ValueMaker}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class DummyValueMaker implements ValueMaker {
	private String returnValue = null;
	private Set<ProjectionSpec> projectionSpecs;
	private Expression selectCondition = Expression.TRUE;
 
	public DummyValueMaker(String value) {
		this.returnValue = value;
	}

	public void describeSelf(NodeSetFilter c) {
	}

	public void setValue(String value) {
		this.returnValue = value;
	}

	public void setProjectionSpecs(Set<ProjectionSpec> columns) {
		this.projectionSpecs = columns;
	}

	public void setSelectCondition(Expression selectCondition) {
		this.selectCondition = selectCondition;
	}

	public boolean matches(String value) {
		return true;
	}

	public Set<ProjectionSpec> projectionSpecs() {
		return this.projectionSpecs;
	}

	public Expression valueExpression(String value, DatabaseOp tabular, Vendor vendor) {
		return selectCondition;
	}

	public String makeValue(ResultRow row) {
		return this.returnValue;
	}
	
	public ValueMaker rename(Renamer renamer) {
		return this;
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		return Collections.emptyList();
	}
}
