package org.d2rq.rdb2rdf;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.d2rq.D2RQTestSuite;
import org.d2rq.HSQLDatabase;
import org.d2rq.SystemLoader;
import org.d2rq.pp.PrettyPrinter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

@RunWith(Parameterized.class)
public class DirectMappingTest {
	private static final String TEST_SUITE_DIR = "test/rdb2rdf-tests/";
	private static final String BASE_URI = "http://example.com/base/";

	@Parameters(name="{index}: {0}")
	public static Collection<Object[]> getTestLists() {
		Collection<Object[]> results = new ArrayList<Object[]>();
		for (File f: new File(TEST_SUITE_DIR).listFiles()) {
			if (!f.isDirectory() || !new File(f.getAbsolutePath() + "/directGraph.ttl").exists()) continue;
			results.add(new Object[]{
					f.getPath(), 
					f.getAbsolutePath() + "/create.sql", 
					f.getAbsolutePath() + "/directGraph.ttl"});
		}
		return results;
	}

	private final String sqlFile;
	private final String resultFile;
	private HSQLDatabase db;
	
	public DirectMappingTest(String name, String sqlFile, String resultFile) {
		this.sqlFile = sqlFile;
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
		loader.setMappingFileOrJdbcURL(db.getJdbcURL());
		loader.setUsername(db.getUser());
		loader.setGenerateW3CDirectMapping(true);
		loader.setStartupSQLScript(sqlFile);
		loader.setSystemBaseURI(BASE_URI);
		Model actualTriples = loader.getModelD2RQ();
		Model expectedTriples = FileManager.get().loadModel(resultFile);
		if (!actualTriples.isIsomorphicWith(expectedTriples)) {
			Model missingStatements = expectedTriples.difference(actualTriples);
			Model unexpectedStatements = actualTriples.difference(expectedTriples);
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
