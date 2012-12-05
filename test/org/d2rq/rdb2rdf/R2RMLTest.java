package org.d2rq.rdb2rdf;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.d2rq.D2RQTestSuite;
import org.d2rq.HSQLDatabase;
import org.d2rq.SystemLoader;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.r2rml.MappingValidator;
import org.d2rq.r2rml.R2RMLReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

@RunWith(Parameterized.class)
public class R2RMLTest {
	private static final String TEST_SUITE_DIR = "test/rdb2rdf-tests/";
	private static final String BASE_URI = "http://example.com/base/";
	private static final Query QUERY = QueryFactory.create(
			"PREFIX rdb2rdftest: <http://purl.org/NET/rdb2rdf-test#> " +
			"SELECT ?s ?rml ?nquad { " +
			"?s a rdb2rdftest:R2RML ." +
			"?s rdb2rdftest:mappingDocument ?rml ." +
			"OPTIONAL {?s rdb2rdftest:output ?nquad} ." +
			"}");

	@Parameters(name="{index}: {0}")
	public static Collection<Object[]> getAllTestLists() {
		Collection<Object[]> results = new ArrayList<Object[]>();
		for (File dir: new File(TEST_SUITE_DIR).listFiles()) {
			if (!dir.isDirectory() || !new File(dir.getAbsolutePath() + "/manifest.ttl").exists()) continue;
			String absolutePath = dir.getAbsolutePath();
			Model model = FileManager.get().loadModel(absolutePath + "/manifest.ttl");
			ResultSet resultSet = QueryExecutionFactory.create(QUERY, model).execSelect();
			while (resultSet.hasNext()) {
				QuerySolution solution = resultSet.nextSolution();
				results.add(new Object[]{
						PrettyPrinter.toString(solution.getResource("s")),
						absolutePath + "/create.sql",
						absolutePath + "/" + solution.getLiteral("rml").getLexicalForm(),
						(solution.get("nquad") == null) ? 
								null : 
								absolutePath + "/" + solution.getLiteral("nquad").getLexicalForm()
				});
			}
		}
		return results;
	}

	private final String sqlFile;
	private final String mappingFile;
	private final String resultFile;
	private HSQLDatabase db;

	public R2RMLTest(String name, String sqlFile, String mappingFile, String resultFile) {
		this.sqlFile = sqlFile;
		this.mappingFile = mappingFile;
		this.resultFile = resultFile;
	}
	
	@Before
	public void setUp() {
		db = new HSQLDatabase("test");
	}
	
	@After
	public void tearDown() {
		db.close(true);
	}
	
	@Test
	public void run() {
		SystemLoader loader = new SystemLoader();
		loader.setJdbcURL(db.getJdbcURL());
		loader.setUsername(db.getUser());
		loader.setMappingFile(mappingFile);
		loader.setStartupSQLScript(sqlFile);
		loader.setSystemBaseURI(BASE_URI);
		if (resultFile == null) {
			R2RMLReader reader = loader.getR2RMLReader();
			MappingValidator validator = new MappingValidator(
					reader.getMapping(), loader.getSQLConnection());
			validator.setReport(reader.getReport());
			validator.run();
			if (!reader.getReport().hasError()) {
				fail("Expected validation error");
			}
			return;
		}
		Model actualTriples = ModelFactory.createDefaultModel();
		actualTriples.add(loader.getModelD2RQ());
		Model expectedTriples = FileManager.get().loadModel(resultFile, "N-TRIPLES");
		if (!actualTriples.isIsomorphicWith(expectedTriples)) {
			Model missingStatements = expectedTriples.difference(actualTriples);
			Model unexpectedStatements = actualTriples.difference(expectedTriples);
			if (missingStatements.isEmpty() && unexpectedStatements.isEmpty()) {
				fail("Models not isomorphic; expected: " + 
						asNTriples(expectedTriples) +
						" actual: " + asNTriples(actualTriples));
			}
			if (missingStatements.isEmpty()) {
				fail("Unexpected statement(s): " + 
						asNTriples(unexpectedStatements));
			} else if (unexpectedStatements.isEmpty()) {
				fail("Missing statement(s): " + 
						asNTriples(missingStatements));
			} else {
				fail("Missing statement(s): " + 
						asNTriples(missingStatements) + 
						" Unexpected statement(s): " + 
						asNTriples(unexpectedStatements));
			}
		}
	}
	
	private String asNTriples(Model model) {
		return PrettyPrinter.toString(model, D2RQTestSuite.STANDARD_PREFIXES);
	}
}
