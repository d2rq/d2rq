package de.fuberlin.wiwiss.d2rq.values;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

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

	public Set<ProjectionSpec> projectionSpecs() {
		return this.projectionSpecs;
	}

	public Expression valueExpression(String value) {
		return selectCondition;
	}

	public String makeValue(ResultRow row) {
		return this.returnValue;
	}
	
	public ValueMaker renameAttributes(ColumnRenamer renamer) {
		return this;
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		return Collections.emptyList();
	}
}
