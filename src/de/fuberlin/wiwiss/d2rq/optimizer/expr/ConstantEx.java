package de.fuberlin.wiwiss.d2rq.optimizer.expr;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.expr.Constant;

/**
 * Extends a <code>Constant</code> with a <code>Node</code>.
 * 
 * @author G. Mels
 */
public class ConstantEx extends Constant {

	private final Node node;
	
	public ConstantEx(String value, Attribute attributeForTrackingType, Node node) {
		super(value, attributeForTrackingType);
		
		this.node = node;
	}

	public ConstantEx(String value, Node node) {
		super(value);
		
		this.node = node;
	}

	public Node getNode() {
		return node;
	}
	
}
