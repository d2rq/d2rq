package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for D2RQ
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RQTestSuite {
	public static final String DIRECTORY = "test/de/fuberlin/wiwiss/d2rq/";
	public static final String DIRECTORY_URL = "file:" + DIRECTORY;
	public static final String ISWC_MAP = "file:doc/example/mapping-iswc.ttl"; 

	public static void main(String[] args) {
		// Be quiet and leave error reporting to JUnit
		Log4jHelper.turnLoggingOff();
		junit.textui.TestRunner.run(D2RQTestSuite.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("D2RQ Test Suite");
		suite.addTest(de.fuberlin.wiwiss.d2rq.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.algebra.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.csv.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.dbschema.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.download.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.expr.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.find.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.functional_tests.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.helpers.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.map.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.mapgen.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.nodes.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.parser.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.pp.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.sql.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.values.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.engine.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.d2rq_sdb.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.optimizer.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.vocab.AllTests.suite());
		return suite;
	}
	
	public static Model loadTurtle(String fileName) {
		Model m = ModelFactory.createDefaultModel();
		m.read(D2RQTestSuite.DIRECTORY_URL + fileName, "TURTLE");
		return m;
	}
}