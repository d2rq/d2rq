package de.fuberlin.wiwiss.d2rq.map;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;

/**
 * Abstract base class for classes that represent things in
 * the mapping file.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class MapObject {
	private Resource resource;
	
	public MapObject(Resource resource) {
		this.resource = resource;
	}

	public Resource resource() {
		return this.resource;
	}
	
	public abstract void validate() throws D2RQException;
	
	public String toString() {
		return PrettyPrinter.toString(this.resource);
	}
	
	protected void assertNotYetDefined(Object object, Property property, int errorCode) {
		if (object == null) {
			return;
		}
		throw new D2RQException("Duplicate " + PrettyPrinter.toString(property) + 
				" for " + this, errorCode);
	}
	
	protected void assertHasBeenDefined(Object object, Property property, int errorCode) {
		if (object != null) {
			return;
		}
		throw new D2RQException("Missing " + PrettyPrinter.toString(property) + 
				" for " + this, errorCode);
	}
	
	protected void assertArgumentNotNull(Object object, Property property, int errorCode) {
		if (object != null) {
			return;
		}
		throw new D2RQException("Object for " + PrettyPrinter.toString(property) + 
				" not found at " + this, errorCode);
	}
}
