package de.fuberlin.wiwiss.d2rq.map;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;

/**
 * Provides default implementations for various NodeMaker methods.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeMakerBase.java,v 1.4 2006/09/03 17:22:50 cyganiak Exp $
 */
public abstract class NodeMakerBase implements NodeMaker {
	private boolean isUnique;
	
	public NodeMakerBase(boolean isUnique) {
		this.isUnique = isUnique;
	}
	
	public abstract void matchConstraint(NodeConstraint c);

	public abstract boolean couldFit(Node node);

	public abstract Map getColumnValues(Node node);

	public abstract Set getColumns();

	public Set getJoins() {
		return Collections.EMPTY_SET;
	}

	public Expression condition() {
		return Expression.TRUE;
	}
	
	public AliasMap getAliases() {
		return AliasMap.NO_ALIASES;
	}
	
	public abstract Node getNode(String[] row, Map columnNameNumberMap);

	public boolean isUnique() {
		return this.isUnique;
	}
}
