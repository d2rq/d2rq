package org.d2rq.r2rml;

import static org.d2rq.ModelAssert.assertIsomorphic;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.d2rq.HSQLDatabase;
import org.d2rq.ModelAssert;
import org.d2rq.SystemLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

/**
 * Runs R2RML tests, driven by a manifest file.
 * 
 * TODO: See manifest file for more R2RML test cases that should be written
 */
@RunWith(Parameterized.class)
public class ProcessorTest {
	private final static String MANIFEST_FILE = "test/r2rml/processor-test-manifest.ttl";
	private final static String BASE_IRI = "http://example.com/";
		
	private final static String PREFIXES = 
		"PREFIX : <http://d2rq.org/terms/test.ttl#>\n" +
		"PREFIX e: <http://d2rq.org/terms/r2rml-errors.ttl#>\n";
	private final static String TEST_CASE_LIST = PREFIXES +
		"SELECT ?mapping ?schema {\n" +
		"  ?mapping a :R2RMLProcessorTestCase.\n" +
		"  ?mapping :schema ?schema.\n" +
		"}";
	private final static String TEST_CASE_TRIPLES = PREFIXES +
		"CONSTRUCT { ?s ?p ?o } { ?mapping :triple (?s ?p ?o) }";

	@Parameters(name="{index}: {0}")
	public static Collection<Object[]> getTestList() {
		Model m = FileManager.get().loadModel(MANIFEST_FILE);
		IRI baseIRI = IRIFactory.iriImplementation().construct(m.getNsPrefixURI("base"));
		ResultSet rs = QueryExecutionFactory.create(TEST_CASE_LIST, m).execSelect();
		List<Object[]> result = new ArrayList<Object[]>();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			Resource mapping = qs.getResource("mapping");
			Resource schema = qs.getResource("schema");
//			if (!mapping.getLocalName().equals("constant-object.ttl")) continue;
			QueryExecution qe = QueryExecutionFactory.create(TEST_CASE_TRIPLES, m);
			qe.setInitialBinding(qs);
			Model expectedTriples = qe.execConstruct();
			result.add(new Object[]{baseIRI.relativize(mapping.getURI()).toString(), mapping.getURI(), 
					schema.getURI(), expectedTriples});
		}
		return result;
	}
	
	private final String mappingFile;
	private final String schemaFile;
	private final Model expectedTriples;
	private HSQLDatabase db;
	private SystemLoader loader;
	
	public ProcessorTest(String id, String mappingFile, String schemaFile, 
			Model expectedTriples) {
		this.mappingFile = mappingFile;
		this.schemaFile = schemaFile;
		this.expectedTriples = expectedTriples;
	}
	
	@Before
	public void setUpDatabase() {
		db = new HSQLDatabase("test");
		db.executeScript(schemaFile);
		loader = new SystemLoader();
		loader.setMappingFile(mappingFile);
		loader.setJdbcURL(db.getJdbcURL());
		loader.setUsername(db.getUser());
		loader.setPassword(db.getPassword());
		loader.setSystemBaseURI(BASE_IRI);
	}
	
	@After
	public void dropDatabase() {
		db.close(true);
		loader.close();
	}
	
	@Test 
	public void testDump() {
		Model actualTriples = ModelFactory.createDefaultModel();
		actualTriples.add(loader.getModelD2RQ());
		ModelAssert.assertIsomorphic(expectedTriples, actualTriples);
	}
	
	@Test
	public void testAsk() {
		StmtIterator it = expectedTriples.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();
			assertTrue("Missing statement: " + stmt, loader.getModelD2RQ().contains(stmt));
		}
	}
	
	@Test
	public void testReadWrite() {
		StringWriter out = new StringWriter();
		new R2RMLWriter(loader.getR2RMLMapping()).write(out);
		Model parsed = ModelFactory.createDefaultModel();
		parsed.read(new StringReader(out.toString()), 
				loader.getD2RQMapping().getBaseURI(), "TURTLE");
		assertIsomorphic(loader.getMappingModel(), parsed);
	}
}
