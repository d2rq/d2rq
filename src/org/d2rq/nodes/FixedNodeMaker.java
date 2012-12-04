package org.d2rq.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.pp.PrettyPrinter;

import com.hp.hpl.jena.graph.Node;


public class FixedNodeMaker implements NodeMaker {
	private Node node;
	
	public FixedNodeMaker(Node node) {
		this.node = node;
	}
	
	public Node getFixedNode() {
		return node;
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

	public String toString() {
		return "Fixed(" + PrettyPrinter.toString(this.node) + ")";
	}

	public List<OrderSpec> orderSpecs(boolean ascending) {
		return OrderSpec.NONE;
	}

	public void accept(NodeMakerVisitor visitor) {
		visitor.visit(this);
	}
}
