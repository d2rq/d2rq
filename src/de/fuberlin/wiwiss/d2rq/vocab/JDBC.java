package de.fuberlin.wiwiss.d2rq.vocab; 
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Open namespace for JDBC connection properties. A JDBC connection
 * property named <tt>foo</tt> is modelled as an RDF property with
 * URI <tt>http://d2rq.org/terms/jdbc/foo</tt>. Values are plain string
 * literals.
 * 
 * @author Richard Cyganiak
 */
public class JDBC {

	/**
	 * The RDF model that holds the vocabulary terms.
	 */
	private static Model model = ModelFactory.createDefaultModel();

	/** 
	 * The namespace of the vocabulary as a string.
	 */
	public static final String NS = "http://d2rq.org/terms/jdbc/";

	/**
	 * The namespace of the vocabulary as a string.
	 * @see #NS
	 */
	public static String getURI() {
		return NS;
	}

	/**
	 * The namespace of the vocabulary as a resource.
	 */
	public static final Resource NAMESPACE = model.createResource(NS);
}
