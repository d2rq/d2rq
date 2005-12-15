/*
 * $Id: AllTests.java,v 1.3 2005/12/15 12:04:05 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.functional_tests;

import de.fuberlin.wiwiss.d2rq.DBConnectionTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Functional test suite for D2RQ. These are functional tests (as opposed to
 * unit tests). The suite runs different find queries against the ISWC database, using the
 * example map provided with the D2RQ manual. To run the test, you must have either the MySQL
 * or the MS Access version accessible. Maybe you must adapt the connection information at the
 * beginning of the map file to fit your database server.
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class AllTests {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AllTests.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.functional_tests");
		//$JUnit-BEGIN$
		suite.addTestSuite(DBConnectionTest.class);
		suite.addTestSuite(FindTest.class);
		suite.addTestSuite(RDQLTest.class);
		suite.addTestSuite(ModelAPITest.class);
		suite.addTestSuite(RDQLDB2Test.class);
		//$JUnit-END$
		return suite;
	}
}
