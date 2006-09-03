package de.fuberlin.wiwiss.d2rq;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for D2RQ
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RQTestSuite.java,v 1.1 2006/09/03 13:45:46 cyganiak Exp $
 */
public class D2RQTestSuite {
	public static final String DIRECTORY = "file:test/de/fuberlin/wiwiss/d2rq/";
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(D2RQTestSuite.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("D2RQ Test Suite");
		suite.addTest(de.fuberlin.wiwiss.d2rq.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.csv.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.find.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.functional_tests.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.map.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.parser.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.pp.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.rdql.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.sql.AllTests.suite());
		return suite;
	}
}