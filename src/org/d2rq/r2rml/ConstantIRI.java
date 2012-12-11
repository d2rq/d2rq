package org.d2rq.r2rml;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * A string that can be validated as an absolute IRI according to RFC 3987.
 */
public class ConstantIRI extends MappingTerm {

	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @return <code>null</code> if arg is <code>null</code>
	 */
	public static ConstantIRI create(String iri) {
		return iri == null ? null : new ConstantIRI(iri);
	}

	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @return <code>null</code> if arg is <code>null</code>
	 */
	public static ConstantIRI create(Resource iri) {
		return iri == null ? null : new ConstantIRI(iri.getURI());
	}

	private final IRI iri;
	
	private ConstantIRI(String iri) {
		if (iri == null) throw new IllegalArgumentException("iri = null");
		this.iri = factory.create(iri);
	}
	private final static IRIFactory factory = IRIFactory.semanticWebImplementation();
	
	public IRI asJenaIRI() {
		return iri;
	}
	
	public Resource asResource() {
		return ResourceFactory.createResource(iri.toString());
	}
	
	@Override
	public String toString() {
		return iri.toString();
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitTerm(this);
	}
	
	@Override
	public boolean isValid() {
		return !iri.hasViolation(false);
	}
	
	@Override
	public int hashCode() {
		return iri.hashCode() ^ 553;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ConstantIRI)) return false;
		return toString().equals(((ConstantIRI) other).toString()); 
	}
}
