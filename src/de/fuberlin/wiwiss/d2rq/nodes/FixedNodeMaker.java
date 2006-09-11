package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.Collections;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
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

	public void matchConstraint(NodeConstraint c) {
		c.matchFixedNode(this.node);
	}

	public Set projectionColumns() {
		return Collections.EMPTY_SET;
	}

	public NodeMaker selectNode(Node n, MutableRelation relation) {
		if (n.equals(this.node) || n.equals(Node.ANY) || n.isVariable()) {
			return this;
		}
		return NodeMaker.EMPTY;
	}
	
	public NodeMaker renameColumns(ColumnRenamer renamer, MutableRelation relation) {
		relation.renameColumns(renamer);
		return new FixedNodeMaker(node, this.isUnique);
	}
	
	public String toString() {
		return "Fixed(" + PrettyPrinter.toString(this.node) + ")";
	}
}
