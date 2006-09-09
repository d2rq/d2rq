package de.fuberlin.wiwiss.d2rq.map;

import java.util.Collections;
import java.util.Set;

/**
 * Provides default implementations for various NodeMaker methods.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeMakerBase.java,v 1.5 2006/09/09 23:25:15 cyganiak Exp $
 */
public abstract class NodeMakerBase implements NodeMaker {
	private boolean isUnique;
	
	public NodeMakerBase(boolean isUnique) {
		this.isUnique = isUnique;
	}
	
	public Set getJoins() {
		return Collections.EMPTY_SET;
	}

	public Expression condition() {
		return Expression.TRUE;
	}
	
	public AliasMap getAliases() {
		return AliasMap.NO_ALIASES;
	}
	
	public boolean isUnique() {
		return this.isUnique;
	}
}
