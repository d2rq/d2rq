package de.fuberlin.wiwiss.d2rq.plan;

import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;

public class ExecuteTripleRelation implements ExecutionPlanElement {
	private TripleRelation tripleRelation;
	
	public ExecuteTripleRelation(TripleRelation tripleRelation) {
		this.tripleRelation = tripleRelation;
	}
	
	public TripleRelation getTripleRelation() {
		return tripleRelation;
	}
	
	public void visit(ExecutionPlanVisitor visitor) {
		visitor.visit(this);
	}
}
