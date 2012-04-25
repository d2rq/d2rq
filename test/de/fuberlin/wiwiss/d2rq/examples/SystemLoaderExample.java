package de.fuberlin.wiwiss.d2rq.examples;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.map.Mapping;

/**
 * Shows how to use the {@link SystemLoader} to initialize various
 * D2RQ components. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SystemLoaderExample {

	public static void main(String[] args) {
		// First, let's set up an in-memory HSQLDB database,
		// load a simple SQL database into it, and generate
		// a default D2RQ mapping for this database using the
		// W3C Direct Mapping
		SystemLoader loader = new SystemLoader();
		loader.setJdbcURL("jdbc:hsqldb:mem:test");
		loader.setStartupSQLScript("doc/example/simple.sql");
		loader.setGenerateW3CDirectMapping(true);
		Mapping mapping = loader.getMapping();

		// Print some internal stuff that shows how D2RQ maps the
		// database to RDF triples
		for (TripleRelation internal: mapping.compiledPropertyBridges()) {
			System.out.println(internal);
		}

		// Write the contents of the virtual RDF model as N-Triples
		Model model = loader.getModelD2RQ();
		model.write(System.out, "N-TRIPLES");

		// Important -- close the model!
		model.close();
		
		// Now let's load an example mapping file that connects to
		// a MySQL database
		loader = new SystemLoader();
		loader.setMappingFileOrJdbcURL("doc/example/mapping-iswc.ttl");
		loader.setFastMode(true);
		loader.setSystemBaseURI("http://example.com/");

		// Get the virtual model and run a SPARQL query
		model = loader.getModelD2RQ();
		ResultSet rs = QueryExecutionFactory.create(
				"SELECT * {?s ?p ?o} LIMIT 10", model).execSelect();
		while (rs.hasNext()) {
			System.out.println(rs.next());
		}
		
		// Important -- close the model!
		model.close();
	}
}
