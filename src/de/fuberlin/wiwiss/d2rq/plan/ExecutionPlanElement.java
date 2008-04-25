package de.fuberlin.wiwiss.d2rq.plan;


/**
 * A relation, as defined in relational algebra, plus a set of NodeMakers
 * attached to the relation, plus a set of TripleMakers attached to the
 * NodeMakers. Very much work in progress.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ExecutionPlanElement.java,v 1.1 2008/04/25 11:25:05 cyganiak Exp $
 */
public interface ExecutionPlanElement {
	
	public static final ExecutionPlanElement EMPTY = new ExecutionPlanElement() {
		public String toString() { return "ExecutionPlanElement.EMPTY"; }
		public void visit(ExecutionPlanVisitor visitor) { visitor.visitEmptyPlanElement(); }
	};
	
	public void visit(ExecutionPlanVisitor visitor);
}