package org.d2rq.r2rml;

import org.d2rq.r2rml.TermMap.ConstantValuedTermMap;
import org.d2rq.r2rml.TermMap.Position;

import com.hp.hpl.jena.rdf.model.RDFNode;


public class ConstantShortcut extends MappingTerm {

	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @result <code>null</code> if arg is <code>null</code>
	 */
	public static ConstantShortcut create(RDFNode constant) {
		return constant == null ? null : new ConstantShortcut(constant);
	}
	
	private final RDFNode constant;
	private final ConstantValuedTermMap termMap;
	
	private ConstantShortcut(RDFNode constant) {
		this.constant = constant;
		termMap = new ConstantValuedTermMap();
		termMap.setConstant(constant);
	}
	
	@Override
	public String toString() {
		return constant.toString();
	}
	
	public ConstantValuedTermMap asTermMap() {
		return termMap;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		acceptAs(visitor, null);
	}
	
	public void acceptAs(MappingVisitor visitor, Position position) {
		visitor.visitTerm(this, position);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ConstantShortcut)) return false;
		return constant.equals(((ConstantShortcut) other).constant);
	}

	@Override
	public int hashCode() {
		return constant.hashCode() ^ 22;
	}
}
