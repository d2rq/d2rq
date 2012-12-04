package de.fuberlin.wiwiss.d2rq.optimizer.expr;

import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.nodes.NodeMaker;


/**
 * Extends <code>AttributeExpr</code> with a <code>NodeMaker</code>.
 * 
 * @author G. Mels
 */
public class ColumnExprEx extends ColumnExpr {
	
	private final NodeMaker nodeMaker;
	
	public ColumnExprEx(ColumnName column, NodeMaker nodeMaker) {
		super(column);
	
		this.nodeMaker = nodeMaker;
	}

	public NodeMaker getNodeMaker() {
		return nodeMaker;
	}

}
