package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * An abstract {@link NodeMaker} that wraps another node maker without changing any
 * behaviour. To be specialized by concrete subclasses.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: WrappingNodeMaker.java,v 1.3 2006/09/09 23:25:14 cyganiak Exp $
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
	
	public Node getNode(ResultRow row) {
		return this.base.getNode(row);
	}

	public boolean isUnique() {
		return this.base.isUnique();
	}
}
