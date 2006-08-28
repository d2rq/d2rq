package de.fuberlin.wiwiss.d2rq.map;

import java.util.HashSet;
import java.util.Set;

/**
 * A node maker that wraps another node maker and adds some join conditions.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: JoinNodeMaker.java,v 1.2 2006/08/28 19:44:21 cyganiak Exp $
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