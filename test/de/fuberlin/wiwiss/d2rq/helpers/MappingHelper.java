package de.fuberlin.wiwiss.d2rq.helpers;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;

/**
 * Test helper for creating {@link Mapping}s.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MappingHelper {

	/**
	 * Parses a D2RQ mapping from a file located relative to
	 * the {@link D2RQTestSuite} directory.
	 * 
	 * @param testFileName Filename, relative to {@link D2RQTestSuite}'s location
	 * @return A mapping
	 * @throws D2RQException On error during parse
	 */
	public static Mapping readFromTestFile(String testFileName) {
		Model m = ModelFactory.createDefaultModel();
		m.read(D2RQTestSuite.DIRECTORY_URL + testFileName, "TURTLE");
		MapParser result = new MapParser(m, "http://example.org/");
		return result.parse();
	}
}
