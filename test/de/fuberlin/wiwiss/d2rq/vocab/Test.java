package de.fuberlin.wiwiss.d2rq.vocab; 
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Namespace used in D2RQ unit testing.
 * 
 * @author Richard Cyganiak
 * @version $Id: Test.java,v 1.1 2008/04/27 22:42:38 cyganiak Exp $ 
 */
public class Test {
	public static final String NS = "http://d2rq.org/terms/test#";
	public static final Resource DummyDatabase = ResourceFactory.createResource(NS + "DummyDatabase");
}
