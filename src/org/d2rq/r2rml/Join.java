package org.d2rq.r2rml;

import org.d2rq.vocab.RR;



public class Join extends MappingComponent {
	private ColumnNameR2RML child = null;
	private ColumnNameR2RML parent = null;
	
	public ComponentType getType() {
		return ComponentType.JOIN;
	}
	
	public void setChild(ColumnNameR2RML child) {
		this.child = child;
	}
	
	public ColumnNameR2RML getChild() {
		return child;
	}
	
	public void setParent(ColumnNameR2RML parent) {
		this.parent = parent;
	}
	
	public ColumnNameR2RML getParent() {
		return parent;
	}

	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitComponent(this);
		visitor.visitTermProperty(RR.child, child);
		visitor.visitTermProperty(RR.parent, parent);
	}
}
