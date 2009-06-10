package de.fuberlin.wiwiss.d2rq.map;

import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;

/*
 * @author Jšrg Hen§
 * @version $Id: DynamicProperty.java,v 1.2 2009/06/10 20:46:49 fatorange Exp $
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
