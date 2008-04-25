package de.fuberlin.wiwiss.d2rq.values;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Dummy implementation of {@link ValueMaker}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DummyValueSource.java,v 1.5 2008/04/25 16:27:41 cyganiak Exp $
 */
public class DummyValueSource implements ValueMaker {
	private String returnValue = null;
	private Set projectionSpecs;
	private Expression selectCondition = Expression.TRUE;

	public DummyValueSource(String value) {
		this.returnValue = value;
	}

	public void describeSelf(NodeSetFilter c) {
	}

	public void setValue(String value) {
		this.returnValue = value;
	}

	public void setProjectionSpecs(Set columns) {
		this.projectionSpecs = columns;
	}

	public void setSelectCondition(Expression selectCondition) {
		this.selectCondition = selectCondition;
	}

	public Set projectionSpecs() {
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
}
