package de.fuberlin.wiwiss.d2rq.map;


/**
 * A {@link NodeMaker} that adds some SQL WHERE conditions to a wrapped base node maker.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ConditionNodeMaker.java,v 1.2 2006/09/03 17:22:49 cyganiak Exp $
 */
public class ConditionNodeMaker extends WrappingNodeMaker {
	private Expression expression;
	
	public ConditionNodeMaker(NodeMaker base, Expression condition) {
		super(base);
		this.expression = condition.and(base.condition());
	}
	
	public Expression condition() {
		return this.expression;
	}
}
