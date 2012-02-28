package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.Collections;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.RelationalOperators;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A specification for creating RDF nodes out of a database relation.
 * 
 * TODO This probably shouldn't have projectionSpecs(), and probably also not
 * 		isUnique() and renameAttributes(), all of which should be handled by the
 * 		underlying Relation
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface NodeMaker {

	static NodeMaker EMPTY = new NodeMaker() {
		public boolean isUnique() { return true; }
		public Node makeNode(ResultRow tuple) { return null; }
		public void describeSelf(NodeSetFilter c) { c.limitToEmptySet(); }
		public Set projectionSpecs() { return Collections.EMPTY_SET; }
		public NodeMaker selectNode(Node node, RelationalOperators sideEffects) { return this; }
		public NodeMaker renameAttributes(ColumnRenamer renamer) { return this; }
	};
	
	Set projectionSpecs();

	boolean isUnique();

	void describeSelf(NodeSetFilter c);
	
	Node makeNode(ResultRow tuple);

	NodeMaker selectNode(Node node, RelationalOperators sideEffects);

	NodeMaker renameAttributes(ColumnRenamer renamer);
}
