package de.fuberlin.wiwiss.d2rq.vocab; 
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Namespace used in D2RQ unit testing.
 * 
 * @author Richard Cyganiak
 */
public class Test {
	public static final String NS = "http://d2rq.org/terms/test#";
	public static final Resource DummyDatabase = ResourceFactory.createResource(NS + "DummyDatabase");
}
