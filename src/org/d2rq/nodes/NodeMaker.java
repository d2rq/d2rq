package org.d2rq.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.schema.ColumnName;

import com.hp.hpl.jena.graph.Node;



/**
 * A specification for creating RDF nodes out of a database relation.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface NodeMaker {

	public static final NodeMaker EMPTY = new NodeMaker() {
		public Node makeNode(ResultRow tuple) { return null; }
		public void describeSelf(NodeSetFilter c) { c.limitToEmptySet(); }
		public Set<ColumnName> getRequiredColumns() { return Collections.emptySet(); }
		public List<OrderSpec> orderSpecs(boolean ascending) { return Collections.emptyList(); }
		public void accept(NodeMakerVisitor visitor) { visitor.visitEmpty(); }
	};
	
	/**
	 * Returns any column names used by the node maker.
	 */
	Set<ColumnName> getRequiredColumns();

	void describeSelf(NodeSetFilter c);
	
	Node makeNode(ResultRow tuple);

	/**
	 * Returns expressions (with possible ASC/DESC marker) that re necessary
	 * for ordering a relation by the nodes in this NodeMaker. Uses SPARQL
	 * semantics for ordering.
	 */
	List<OrderSpec> orderSpecs(boolean ascending);
	
	void accept(NodeMakerVisitor visitor);
}
