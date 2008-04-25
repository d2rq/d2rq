package de.fuberlin.wiwiss.d2rq.plan;


public interface ExecutionPlanVisitor {

	void visit(ExecuteTripleRelation tripleRelation);
	
	void visit(ExecuteCompatibleTripleRelations compatibleRelations);
	
	void visit(ExecuteSequence sequence);
	
	void visitEmptyPlanElement();
}
