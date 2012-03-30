package de.fuberlin.wiwiss.d2rq.optimizer.expr;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.expr.AttributeExpr;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;

/**
 * Extends <code>AttributeExpr</code> with a <code>NodeMaker</code>.
 * 
 * @author G. Mels
 */
public class AttributeExprEx extends AttributeExpr {
	
	private final NodeMaker nodeMaker;
	
	public AttributeExprEx(Attribute attribute, NodeMaker nodeMaker) {
		super(attribute);
	
		this.nodeMaker = nodeMaker;
	}

	public NodeMaker getNodeMaker() {
		return nodeMaker;
	}

}
