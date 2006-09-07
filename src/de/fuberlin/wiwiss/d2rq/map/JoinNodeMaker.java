package de.fuberlin.wiwiss.d2rq.map;

import java.util.HashSet;
import java.util.Set;

/**
 * A node maker that wraps another node maker and adds some join conditions.
 * 
 * TODO: Pass in just one join instead of a set, use multiple wrapped if you have multiple joins
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: JoinNodeMaker.java,v 1.3 2006/09/07 15:14:27 cyganiak Exp $
 */
public class JoinNodeMaker extends WrappingNodeMaker {
	private Set joins;
	private boolean isUnique;
	
	public JoinNodeMaker(NodeMaker base, Set joins, boolean isUnique) {
		super(base);
		this.joins = new HashSet(joins);
		joins.addAll(base.getJoins());
		this.isUnique = isUnique;
	}
	
	public Set getJoins() {
		return this.joins;
	}
	
	public boolean isUnique() {
		return this.isUnique;
	}
}