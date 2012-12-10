package org.d2rq.lang;

import org.d2rq.D2RQException;
import org.d2rq.pp.PrettyPrinter;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Abstract base class for classes that represent things in
 * the mapping file.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class MapObject {
	private Resource resource;
	private String comment = null;
	
	public MapObject(Resource resource) {
		this.resource = resource;
	}

	public Resource resource() {
		return this.resource;
	}

	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public abstract void accept(D2RQMappingVisitor visitor);

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
	
	protected void assertArgumentNotNull(Object object, Property property, int errorCode) {
		if (object != null) {
			return;
		}
		throw new D2RQException("Object for " + PrettyPrinter.toString(property) + 
				" not found at " + this, errorCode);
	}
}
