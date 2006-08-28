package de.fuberlin.wiwiss.d2rq.map;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link NodeMaker} that adds some SQL WHERE conditions to a wrapped base node maker.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ConditionNodeMaker.java,v 1.1 2006/08/28 19:44:21 cyganiak Exp $
 */
public class ConditionNodeMaker extends WrappingNodeMaker {
	private Set conditions;
	
	public ConditionNodeMaker(NodeMaker base, Set conditions) {
		super(base);
		this.conditions = new HashSet(conditions);
		this.conditions.addAll(base.getConditions());
	}
	
	public Set getConditions() {
		return this.conditions;
	}
}
