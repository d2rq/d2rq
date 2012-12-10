package org.d2rq.mapgen;

import static org.d2rq.ModelAssert.assertIsomorphic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.d2rq.HSQLDatabase;
import org.d2rq.SystemLoader;
import org.d2rq.validation.Report;
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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

@RunWith(Parameterized.class)
public class MappingGeneratorTest {
	private final static String MANIFEST_FILE = "test/mapgen/mapgen-test-manifest.ttl";
	private final static String BASE_IRI = "http://example.com/";

	private final static String PREFIXES = 
			"PREFIX : <http://d2rq.org/terms/test.ttl#>\n";
		private final static String TEST_CASE_LIST = PREFIXES +
			"SELECT ?case ?sql {\n" +
			"  ?case a :MappingGeneratorTestCase.\n" +
			"  ?case :sql ?sql.\n" +
			"}";
		private final static String TEST_CASE_TRIPLES = PREFIXES +
				"CONSTRUCT { ?s ?p ?o } { ?case :triple (?s ?p ?o) }";
		
	@Parameters(name="{index}: {0}")
	public static Collection<Object[]> getTestLists() {
		Model m = FileManager.get().loadModel(MANIFEST_FILE);
		IRI baseIRI = IRIFactory.iriImplementation().construct(m.getNsPrefixURI("base"));
		ResultSet rs = QueryExecutionFactory.create(TEST_CASE_LIST, m).execSelect();
		List<Object[]> result = new ArrayList<Object[]>();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			Resource testCase = qs.getResource("case");
//			if (!case.getLocalName().equals("expression")) continue;
			QueryExecution qe = QueryExecutionFactory.create(TEST_CASE_TRIPLES, m);
			qe.setInitialBinding(qs);
			Model expectedTriples = qe.execConstruct();
			result.add(new Object[]{
					baseIRI.relativize(testCase.getURI()).toString(), 
					qs.getLiteral("sql").getLexicalForm(), 
					expectedTriples});
		}
		return result;
	}

	private final String sql;
	private final Model expectedTriples;
	private HSQLDatabase db;
	private SystemLoader loader;
	
	public MappingGeneratorTest(String id, String sql, Model expectedTriples) {
		this.sql = sql;
		this.expectedTriples = expectedTriples;
	}
	
	@Before
	public void setUp() throws SQLException {
		db = new HSQLDatabase("test");
		db.executeSQL(sql);
		loader = new SystemLoader();
		loader.setJdbcURL(db.getJdbcURL());
		loader.setUsername(db.getUser());
		loader.setPassword(db.getPassword());
		loader.setSystemBaseURI(BASE_IRI);
		loader.setServeVocabulary(false);
		loader.getMappingGenerator().setSuppressWarnings(true);
		loader.setReport(new Report());	// override default behaviour of logging warnings
	}
	
	@After
	public void tearDown() {
		db.close(true);
		loader.close();
	}
	
	@Test
	public void run() {
		Model actualTriples = loader.getModelD2RQ();
		expectedTriples.setNsPrefixes(actualTriples);
		assertIsomorphic(expectedTriples, actualTriples);
	}
}
