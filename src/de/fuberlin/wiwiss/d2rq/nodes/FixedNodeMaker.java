package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.RelationalOperators;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

public class FixedNodeMaker implements NodeMaker {
	private Node node;
	private boolean isUnique;
	
	public FixedNodeMaker(Node node, boolean isUnique) {
		this.node = node;
		this.isUnique = isUnique;
	}
	
	public boolean isUnique() {
		return this.isUnique;
	}

	public Node makeNode(ResultRow tuple) {
		return this.node;
	}

	public void describeSelf(NodeSetFilter c) {
		c.limitTo(this.node);
	}

	public Set<ProjectionSpec> projectionSpecs() {
		return Collections.<ProjectionSpec>emptySet();
	}

	public NodeMaker selectNode(Node n, RelationalOperators sideEffects) {
		if (n.equals(this.node) || n.equals(Node.ANY) || n.isVariable()) {
			return this;
		}
		sideEffects.select(Expression.FALSE);
		return NodeMaker.EMPTY;
	}
	
	public NodeMaker renameAttributes(ColumnRenamer renamer) {
		return new FixedNodeMaker(node, this.isUnique);
	}
	
	public String toString() {
		return "Fixed(" + PrettyPrinter.toString(this.node) + ")";
	}

	public List<OrderSpec> orderSpecs(boolean ascending) {
		return Collections.<OrderSpec>emptyList();
	}
}
