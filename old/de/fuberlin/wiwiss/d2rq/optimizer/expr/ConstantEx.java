package de.fuberlin.wiwiss.d2rq.optimizer.expr;

import org.d2rq.db.expr.Constant;
import org.d2rq.db.types.DataType;

import com.hp.hpl.jena.graph.Node;


/**
 * Extends a <code>Constant</code> with a <code>Node</code>.
 * 
 * @author G. Mels
 */
public class ConstantEx extends Constant {

	private final Node node;
	
	public ConstantEx(String value, DataType dataType, Node node) {
		super(value, dataType);
		
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
