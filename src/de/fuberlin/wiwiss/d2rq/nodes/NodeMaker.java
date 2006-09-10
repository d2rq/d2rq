package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.Collections;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.map.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

public interface NodeMaker {

	static NodeMaker EMPTY = new NodeMaker() {
		public boolean isUnique() { return true; }
		public Node makeNode(ResultRow tuple) { return null; }
		public void matchConstraint(NodeConstraint c) { c.matchImpossible(); }
		public Set projectionColumns() { return Collections.EMPTY_SET; }
		public NodeMaker selectNode(Node node, MutableRelation relation) { return this; }
		public NodeMaker renameColumns(ColumnRenamer renamer, MutableRelation relation) { return this; }
	};
	
	Set projectionColumns();

	boolean isUnique();

	void matchConstraint(NodeConstraint c);
	
	Node makeNode(ResultRow tuple);

	NodeMaker selectNode(Node node, MutableRelation relation);

	NodeMaker renameColumns(ColumnRenamer renamer, MutableRelation relation);
}
