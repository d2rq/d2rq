package de.fuberlin.wiwiss.d2rq.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecuteSequence implements ExecutionPlanElement {
	private final List elements = new ArrayList();
	
	public ExecuteSequence() {
		this(Collections.EMPTY_LIST);
	}
	
	public ExecuteSequence(List elements) {
		this.elements.addAll(elements);
	}
	
	public void add(ExecutionPlanElement element) {
		this.elements.add(element);
	}
	
	public List elements() {
		return Collections.unmodifiableList(elements);
	}
	
	public void visit(ExecutionPlanVisitor visitor) {
		visitor.visit(this);
	}
}
