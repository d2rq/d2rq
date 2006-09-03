package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;

/**
 * An abstract {@link NodeMaker} that wraps another node maker without changing any
 * behaviour. To be specialized by concrete subclasses.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: WrappingNodeMaker.java,v 1.2 2006/09/03 17:22:50 cyganiak Exp $
 */
public abstract class WrappingNodeMaker implements NodeMaker {
	protected NodeMaker base;
	
	public WrappingNodeMaker(NodeMaker base) {
		this.base = base;
	}
	
	public void matchConstraint(NodeConstraint c) {
		this.base.matchConstraint(c);
	}

	public boolean couldFit(Node node) {
		return this.base.couldFit(node);
	}

	public Map getColumnValues(Node node) {
		return this.base.getColumnValues(node);
	}

	public Set getColumns() {
		return this.base.getColumns();
	}

	public Set getJoins() {
		return this.base.getJoins();
	}

	public Expression condition() {
		return this.base.condition();
	}
	
	public AliasMap getAliases() {
		return this.base.getAliases();
	}
	
	public Node getNode(String[] row, Map columnNameNumberMap) {
		return this.base.getNode(row, columnNameNumberMap);
	}

	public boolean isUnique() {
		return this.base.isUnique();
	}
}
