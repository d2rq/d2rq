package de.fuberlin.wiwiss.d2rq;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for D2RQ
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RQTestSuite.java,v 1.10 2006/11/02 20:46:46 cyganiak Exp $
 */
public class D2RQTestSuite {
	public static final String DIRECTORY = "test/de/fuberlin/wiwiss/d2rq/";
	public static final String DIRECTORY_URL = "file:" + DIRECTORY;
	public static final String ISWC_MAP = "file:doc/example/mapping-iswc.n3"; 

	public static void main(String[] args) {
		junit.textui.TestRunner.run(D2RQTestSuite.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("D2RQ Test Suite");
		suite.addTest(de.fuberlin.wiwiss.d2rq.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.algebra.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.csv.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.expr.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.fastpath.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.find.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.functional_tests.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.map.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.nodes.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.parser.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.pp.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.rdql.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.sql.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.values.AllTests.suite());
		return suite;
	}
}