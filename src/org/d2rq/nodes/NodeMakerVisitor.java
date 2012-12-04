package org.d2rq.nodes;

import org.d2rq.nodes.NodeMaker.EmptyNodeMaker;

public interface NodeMakerVisitor {

	void visit(EmptyNodeMaker nodeMaker);
	
	void visit(FixedNodeMaker nodeMaker);
	
	void visit(TypedNodeMaker nodeMaker);
}
