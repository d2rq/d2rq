package org.d2rq.nodes;


public interface NodeMakerVisitor {

	void visitEmpty();
	
	void visit(FixedNodeMaker nodeMaker);
	
	void visit(TypedNodeMaker nodeMaker);
}
