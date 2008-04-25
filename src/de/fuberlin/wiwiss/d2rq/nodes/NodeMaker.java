package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.Collections;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A specification for creating RDF nodes out of a database relation.
 * 
 * TODO This probably shouldn't have projectionSpecs(), and probably also not
 * 		isUnique() and renameColumns(), all of which should be handled by the
 * 		underlying Relation
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeMaker.java,v 1.5 2008/04/25 15:26:20 cyganiak Exp $
 */
public interface NodeMaker {

	static NodeMaker EMPTY = new NodeMaker() {
		public boolean isUnique() { return true; }
		public Node makeNode(ResultRow tuple) { return null; }
		public void describeSelf(NodeSetFilter c) { c.limitToEmptySet(); }
		public Set projectionSpecs() { return Collections.EMPTY_SET; }
		public NodeMaker selectNode(Node node, MutableRelation relation) { return this; }
		public NodeMaker renameAttributes(ColumnRenamer renamer, MutableRelation relation) { return this; }
	};
	
	Set projectionSpecs();

	boolean isUnique();

	void describeSelf(NodeSetFilter c);
	
	Node makeNode(ResultRow tuple);

	NodeMaker selectNode(Node node, MutableRelation relation);

	NodeMaker renameAttributes(ColumnRenamer renamer, MutableRelation relation);
}
