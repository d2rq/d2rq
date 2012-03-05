package de.fuberlin.wiwiss.d2rq.map;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;

/**
 * @author J&ouml;rg Hen&szlig;
 */
public class DynamicProperty extends ResourceMap {
	
	public DynamicProperty(String URI) {
		super(null, true);
		this.setURIPattern(URI);
	}

	//@Override
	protected Relation buildRelation() {
		return null;
	}

	//@Override
	public void validate() throws D2RQException {
		// TODO Auto-generated method stub
	}

	public String toString() {
		return "d2rq:dynamicProperty '" + (this.uriPattern != null ? this.uriPattern.toString() : "") + "'";		
	}
}
