package de.fuberlin.wiwiss.d2rq.values;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A pseudo value maker that produces a constant value
 * regardless of the underlying relation.
 * 
 * Note that {@link FixedNodeMaker} doesn't use an instance
 * of this class, but handles the constantness of its
 * underlying values directly.
 * 
 * This class is only used where we need to produce constant
 * strings, rather than constant RDF nodes.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ConstantValueMaker implements ValueMaker {
	private String value;
	
	public ConstantValueMaker(String constant) {
		this.value = constant;
	}
	public Expression valueExpression(String value) {
		return this.value.equals(value) ? Expression.TRUE : Expression.FALSE;
	}

	public Set<ProjectionSpec> projectionSpecs() {
		return Collections.emptySet();
	}

	public String makeValue(ResultRow row) {
		return value;
	}

	public void describeSelf(NodeSetFilter c) {
		c.limitValues(value);
	}

	public ValueMaker renameAttributes(ColumnRenamer renamer) {
		return this;
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		return Collections.emptyList();
	}
}
