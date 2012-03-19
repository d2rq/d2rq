package de.fuberlin.wiwiss.d2rq.functional_tests;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Functional test suite for D2RQ. These are functional tests (as opposed to
 * unit tests). The suite runs different find queries against the ISWC database, using the
 * example map provided with the D2RQ manual. To run the test, you must have either the MySQL
 * or the MS Access version accessible. Maybe you must adapt the connection information at the
 * beginning of the map file to fit your database server.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class AllTests {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AllTests.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.functional_tests");
		//$JUnit-BEGIN$
		suite.addTestSuite(FindTest.class);
		suite.addTestSuite(SPARQLTest.class);
		suite.addTestSuite(ModelAPITest.class);
		//$JUnit-END$
		return suite;
	}
}
