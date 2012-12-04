package org.d2rq.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;

import com.hp.hpl.jena.graph.Node;



/**
 * A specification for creating RDF nodes out of a database relation.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface NodeMaker {

	public static NodeMaker EMPTY = new EmptyNodeMaker();
	
	Set<ProjectionSpec> projectionSpecs();

	void describeSelf(NodeSetFilter c);
	
	Node makeNode(ResultRow tuple);

	/**
	 * Returns expressions (with possible ASC/DESC marker) that re necessary
	 * for ordering a relation by the nodes in this NodeMaker. Uses SPARQL
	 * semantics for ordering.
	 */
	List<OrderSpec> orderSpecs(boolean ascending);
	
	void accept(NodeMakerVisitor visitor);
	
	public static class EmptyNodeMaker implements NodeMaker {
		/** Use {@link NodeMaker#EMPTY} instead */
		private EmptyNodeMaker() {}
		public Node makeNode(ResultRow tuple) { return null; }
		public void describeSelf(NodeSetFilter c) { c.limitToEmptySet(); }
		public Set<ProjectionSpec> projectionSpecs() { return Collections.<ProjectionSpec>emptySet(); }
		public List<OrderSpec> orderSpecs(boolean ascending) { return Collections.<OrderSpec>emptyList(); }
		public void accept(NodeMakerVisitor visitor) { visitor.visit(this); }
	};
}
