package de.fuberlin.wiwiss.d2rq;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for D2RQ
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AllTests.java,v 1.14 2006/09/03 12:57:30 cyganiak Exp $
 */
public class AllTests {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AllTests.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq");
		//$JUnit-BEGIN$
		suite.addTestSuite(DBConnectionTest.class);
		//$JUnit-END$
		suite.addTest(de.fuberlin.wiwiss.d2rq.parser.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.map.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.find.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.rdql.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.sql.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.pp.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.csv.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.functional_tests.AllTests.suite());
		return suite;
	}
}