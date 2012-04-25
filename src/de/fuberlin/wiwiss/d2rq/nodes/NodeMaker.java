package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.RelationalOperators;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A specification for creating RDF nodes out of a database relation.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface NodeMaker {

	static NodeMaker EMPTY = new NodeMaker() {
		public boolean isUnique() { return true; }
		public Node makeNode(ResultRow tuple) { return null; }
		public void describeSelf(NodeSetFilter c) { c.limitToEmptySet(); }
		public Set<ProjectionSpec> projectionSpecs() { return Collections.<ProjectionSpec>emptySet(); }
		public NodeMaker selectNode(Node node, RelationalOperators sideEffects) { return this; }
		public NodeMaker renameAttributes(ColumnRenamer renamer) { return this; }
		public List<OrderSpec> orderSpecs(boolean ascending) { return Collections.<OrderSpec>emptyList(); }
	};
	
	Set<ProjectionSpec> projectionSpecs();

	boolean isUnique();

	void describeSelf(NodeSetFilter c);
	
	Node makeNode(ResultRow tuple);

	NodeMaker selectNode(Node node, RelationalOperators sideEffects);

	NodeMaker renameAttributes(ColumnRenamer renamer);
	
	/**
	 * Returns expressions (with possible ASC/DESC marker) that re necessary
	 * for ordering a relation by the nodes in this NodeMaker. Uses SPARQL
	 * semantics for ordering.
	 */
	List<OrderSpec> orderSpecs(boolean ascending);
}
